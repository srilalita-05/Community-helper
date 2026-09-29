package com.communityos.maintenance.event

sealed class BillDetailsEvent {
    object Refresh : BillDetailsEvent()
    object DismissError : BillDetailsEvent()
}
