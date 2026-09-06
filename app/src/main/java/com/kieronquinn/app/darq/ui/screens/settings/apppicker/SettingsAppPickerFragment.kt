package com.kieronquinn.app.darq.ui.screens.settings.apppicker

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.darq.R
import com.kieronquinn.app.darq.databinding.FragmentAppPickerBinding
import com.kieronquinn.app.darq.model.settings.AppPickerItem
import com.kieronquinn.app.darq.ui.base.BackAvailable
import com.kieronquinn.app.darq.ui.base.BoundFragment
import com.kieronquinn.app.darq.ui.screens.container.ContainerSharedViewModel
import com.kieronquinn.app.darq.ui.utils.TransitionUtils
import com.kieronquinn.app.darq.utils.extensions.applyMD3SwitchMonet
import com.kieronquinn.app.darq.utils.extensions.expandAppBar
import com.kieronquinn.app.darq.utils.extensions.navGraphViewModel
import com.kieronquinn.monetcompat.extensions.views.applyMonetRecursively
import com.kieronquinn.monetcompat.extensions.views.enableStretchOverscroll
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


class SettingsAppPickerFragment :
    BoundFragment<FragmentAppPickerBinding>(FragmentAppPickerBinding::inflate), BackAvailable {

    private val viewModel by viewModel<SettingsAppPickerViewModel>()
    private val sharedViewModel by navGraphViewModel<ContainerSharedViewModel>(R.id.nav_graph_main)

    private var isFilterExpanded = false

    private val adapter by lazy {
        SettingsAppPickerAdapter(
            requireContext(),
            emptyList<AppPickerItem>().toMutableList(),
            this::onPackageEnabledChanged
        )
    }

    private val searchTextWatcher = object : TextWatcher {

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val query = s?.toString() ?: ""
            viewModel.setSearchTerm(query)
            binding.filterContainer.isVisible = query.isEmpty()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun afterTextChanged(s: Editable?) {}

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), true)
        enterTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), true)
        returnTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), false)
        reenterTransition = TransitionUtils.getMaterialSharedAxis(requireContext(), false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.applyMonetRecursively()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setLoadingState(loading = true, isEmpty = false)
        setupFilterPanel()
        setupRecyclerView()
        setupViewModel()
        setupSearch()
        setupSnackbarPadding()
    }

    private fun setupFilterPanel() = with(binding) {
        val prefs = requireContext().getSharedPreferences("darq_ui_prefs", Context.MODE_PRIVATE)
        isFilterExpanded = prefs.getBoolean("filter_expanded", false)

        filterSwitch.applyMD3SwitchMonet()
        filterSwitch.setOnCheckedChangeListener(null)
        filterSwitch.isChecked = viewModel.getShowAllApps()
        filterSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowAllApps(isChecked)
        }

        updateFilterExpansion(animate = false)

        val toggleExpansion = {
            isFilterExpanded = !isFilterExpanded
            prefs.edit().putBoolean("filter_expanded", isFilterExpanded).apply()
            updateFilterExpansion(animate = true)
        }

        filterHeaderArea.setOnClickListener { toggleExpansion() }

        filterContainer.setOnClickListener {
            if (!isFilterExpanded) {
                toggleExpansion()
            }
        }
    }

    private fun updateFilterExpansion(animate: Boolean) = with(binding) {
        val duration = if (animate) 220L else 0L
        if (animate) {
            filterArrow.animate().rotation(if (isFilterExpanded) 180f else 0f).setDuration(duration).start()
        } else {
            filterArrow.rotation = if (isFilterExpanded) 180f else 0f
        }
        filterHeaderArea.background = ContextCompat.getDrawable(
            requireContext(),
            if (isFilterExpanded) R.drawable.bg_filter_header_ripple
            else R.drawable.bg_group_card_single
        )

        if (isFilterExpanded) {
            filterSwitch.visibility = View.VISIBLE
            if (animate) {
                filterSwitch.alpha = 0f
                filterSwitch.translationX = 50f
                filterSwitch.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(duration)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            } else {
                filterSwitch.alpha = 1f
                filterSwitch.translationX = 0f
            }
        } else {
            if (animate) {
                filterSwitch.animate()
                    .alpha(0f)
                    .translationX(50f)
                    .setDuration(180L)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction {
                        filterSwitch.visibility = View.GONE
                    }
                    .start()
            } else {
                filterSwitch.visibility = View.GONE
                filterSwitch.alpha = 0f
            }
        }
    }

    private fun setupRecyclerView() {
        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SettingsAppPickerFragment.adapter
            ViewCompat.setOnApplyWindowInsetsListener(this){ view, insets ->
                val requiredInsets = insets.getInsets(WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.statusBars())
                updatePadding(left = requiredInsets.left, right = requiredInsets.right, bottom = requiredInsets.bottom)
                insets
            }
            enableStretchOverscroll()
        }
    }

    private fun setupViewModel() {
        lifecycleScope.launchWhenResumed {
            launch {
                viewModel.loadState.debounce(50).collect {
                    handleLoadState(it)
                }
            }
        }
    }

    private fun handleLoadState(loadState: SettingsAppPickerViewModel.LoadState) {
        setLoadingState(
            loadState is SettingsAppPickerViewModel.LoadState.Loading,
            (loadState is SettingsAppPickerViewModel.LoadState.Loaded && loadState.apps.isEmpty())
        )
        if (loadState is SettingsAppPickerViewModel.LoadState.Loaded) {
            adapter.setItems(loadState.apps)
        }
    }

    private fun setLoadingState(loading: Boolean, isEmpty: Boolean) {
        binding.recyclerView.isVisible = !loading && !isEmpty
        binding.appPickerLoading.isVisible = loading && !isEmpty
        binding.appPickerEmpty.isVisible = isEmpty
    }

    private fun setupSearch() {
        with(binding.appPickerSearch) {
            val background = monet.getBackgroundColor(requireContext())
            val cardBg = ContextCompat.getColor(requireContext(), R.color.card_background)
            root.setBackgroundColor(background)
            searchBox.backgroundTintList = ColorStateList.valueOf(cardBg)
            searchBox.text.run {
                clear()
                append(viewModel.getSearchTerm())
            }
            searchBox.setOnClickListener {
                expandAppBar()
            }
            lifecycleScope.launchWhenResumed {
                viewModel.showSearchClearButton.collect {
                    searchClear.isVisible = it
                }
            }
            searchClear.setOnClickListener {
                searchBox.text.clear()
            }
        }
        binding.filterContainer.isVisible = viewModel.getSearchTerm().isEmpty()
    }

    override fun onResume() {
        super.onResume()
        binding.appPickerSearch.searchBox.addTextChangedListener(searchTextWatcher)
    }

    override fun onPause() {
        super.onPause()
        binding.appPickerSearch.searchBox.removeTextChangedListener(searchTextWatcher)
    }

    private fun onPackageEnabledChanged(appPickerItem: AppPickerItem.App){
        viewModel.onPackageEnabledChanged(appPickerItem)
        sharedViewModel.queueIPCSync(appPickerItem.toIPCSetting())
    }

    private fun setupSnackbarPadding(){
        lifecycleScope.launchWhenResumed {
            sharedViewModel.showSnackbar.collect {
                if(it){
                    adapter.addSnackbarPadding()
                }else{
                    adapter.removeSnackbarPadding()
                }
            }
        }
    }

}