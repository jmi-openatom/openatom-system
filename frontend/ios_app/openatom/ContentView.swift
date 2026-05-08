import SwiftUI

struct ContentView: View {
    @Environment(Session.self) private var session
    @State private var selectedTab: Tab = .home

    var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(Tab.allCases) { tab in
                NavigationStack {
                    tab.view
                }
                .tabItem {
                    Label(tab.title, systemImage: tab.icon)
                }
                .badge(tab == .profile ? session.unreadCount : 0)
                .tag(tab)
            }
        }
    }

}

extension ContentView {
    enum Tab: CaseIterable, Identifiable {
        case home
        case discover
        case recruitment
        case profile
        case admin

        var id: Self { self }

        var title: String {
            switch self {
            case .home:        return "首页"
            case .discover:    return "活动"
            case .recruitment: return "申请"
            case .profile:     return "我的"
            case .admin:       return "管理"
            }
        }

        var icon: String {
            switch self {
            case .home:        return "house.fill"
            case .discover:    return "calendar.fill"
            case .recruitment: return "person.badge.plus"
            case .profile:     return "person.fill"
            case .admin:       return "gearshape.fill"
            }
        }

        @ViewBuilder
        var view: some View {
            switch self {
            case .home:        HomeView()
            case .discover:    DiscoverView()
            case .recruitment: RecruitmentView()
            case .profile:     ProfileView()
            case .admin:       SettingsView()
            }
        }
    }
}
