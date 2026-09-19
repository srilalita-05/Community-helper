package com.communityos.navigation

sealed class Screen(val route: String) {
    // Auth Flow
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object OtpVerification : Screen("otp_verification/{phone}") {
        fun createRoute(phone: String) = "otp_verification/$phone"
    }
    object Registration : Screen("registration")
    object CommunitySelection : Screen("community_selection")
    object FlatVerification : Screen("flat_verification")

    // Dashboards
    object ResidentDashboard : Screen("resident_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
    object SecurityDashboard : Screen("security_dashboard")

    // Sub-features / Details (Can be launched from dashboards)
    object VisitorDetails : Screen("visitor_details/{visitorId}") {
        fun createRoute(visitorId: String) = "visitor_details/$visitorId"
    }
    object ClubDetails : Screen("club_details/{clubId}") {
        fun createRoute(clubId: String) = "club_details/$clubId"
    }

    // Resident Notices Flow
    object NoticesList : Screen("notices_list")
    object NoticeDetails : Screen("notice_details/{noticeId}") {
        fun createRoute(noticeId: String) = "notice_details/$noticeId"
    }

    // Resident Complaints Flow
    object ComplaintsList : Screen("complaints_list")
    object CreateComplaint : Screen("create_complaint")
    object ComplaintDetails : Screen("complaint_details/{complaintId}") {
        fun createRoute(complaintId: String) = "complaint_details/$complaintId"
    }
}
