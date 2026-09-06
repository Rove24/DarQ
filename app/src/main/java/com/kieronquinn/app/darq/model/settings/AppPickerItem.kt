package com.kieronquinn.app.darq.model.settings

sealed class AppPickerItem(open val itemType: AppPickerItemType) {
    data class Header(val title: CharSequence): AppPickerItem(AppPickerItemType.HEADER)
    data class App(val packageName: String, val label: CharSequence, var enabled: Boolean): AppPickerItem(AppPickerItemType.APP){
        fun toIPCSetting(): IPCSetting {
            return IPCSetting(packageChange = IPCPackageChange(packageName, enabled))
        }
    }

    object SnackbarPadding: AppPickerItem(AppPickerItemType.SNACKBAR_PADDING)
}

enum class AppPickerItemType {
    HEADER, APP, SNACKBAR_PADDING
}
