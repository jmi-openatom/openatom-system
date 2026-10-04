import SwiftUI

struct HomeView: View {
    @Environment(Session.self) private var session
    @State private var vm = HomeViewModel()

    var body: some View {
        NavigationStack {
            Group {
                if vm.isLoading && vm.home == nil {
                    LoadingView("加载中...")
                } else if let error = vm.error, vm.home == nil {
                    ErrorView(message: error) { Task { await vm.load() } }
                } else if let home = vm.home {
                    contentView(home)
                } else {
                    EmptyStateView(icon: "house", title: "暂无数据")
                }
            }
            .navigationTitle("首页")
            .navigationBarTitleDisplayMode(.large)
            .refreshable { await vm.load() }
            .task { await vm.load() }
        }
    }

    @ViewBuilder
    private func contentView(_ home: SiteHome) -> some View {
        ScrollView {
            VStack(spacing: 20) {
                if let club = home.club {
                    heroSection(club: club, metrics: home.metrics ?? [])
                }

                VStack(spacing: 20) {
                    if let techStack = home.techStack, !techStack.isEmpty {
                        techStackSection(techStack)
                    }

                    if let focusAreas = home.focusAreas, !focusAreas.isEmpty {
                        focusAreasSection(focusAreas)
                    }

                    if let activities = home.activities, !activities.isEmpty {
                        activitiesSection(activities)
                    }

                    if let people = home.people, !people.isEmpty {
                        peopleSection(people)
                    }

                    if let awards = home.awards, !awards.isEmpty {
                        awardsSection(awards)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
        .background(Color(.systemGroupedBackground))
        .ignoresSafeArea(edges: .top)
    }

    // MARK: - Hero

    private func heroSection(club: SiteClub, metrics: [SiteMetric]) -> some View {
        VStack(spacing: 0) {
            ZStack {
                LinearGradient(
                    colors: [Color.accentColor, Color.accentColor.opacity(0.8), Color.purple.opacity(0.6)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .frame(height: 300)
                
                VStack(spacing: 16) {
                    Spacer(minLength: 60)
                    if let logo = club.logoUrl, !logo.isEmpty {
                        AsyncImageView(url: logo, size: 90)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .shadow(color: .black.opacity(0.15), radius: 10, x: 0, y: 5)
                    } else {
                        Image(systemName: "atom")
                            .font(.system(size: 44))
                            .foregroundColor(.white)
                            .frame(width: 90, height: 90)
                            .background(Color.white.opacity(0.2))
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                    }

                    VStack(spacing: 4) {
                        Text(club.name ?? "社团")
                            .font(.title.bold())
                            .foregroundColor(.white)
                        
                        if let desc = club.description, !desc.isEmpty {
                            Text(desc)
                                .font(.subheadline)
                                .foregroundColor(.white.opacity(0.9))
                                .multilineTextAlignment(.center)
                                .lineLimit(2)
                                .padding(.horizontal, 40)
                        }
                    }
                    
                    if let status = club.recruitmentStatus {
                        StatusBadge(status: status)
                            .shadow(color: .black.opacity(0.1), radius: 5)
                    }
                    Spacer(minLength: 20)
                }
            }
            .clipShape(UnevenRoundedRectangle(bottomLeadingRadius: 40, bottomTrailingRadius: 40))

            if !metrics.isEmpty {
                HStack(spacing: 12) {
                    ForEach(Array(metrics.prefix(3).enumerated()), id: \.offset) { _, metric in
                        VStack(spacing: 4) {
                            Text(metric.value ?? "0")
                                .font(.headline.bold())
                                .foregroundColor(.primary)
                            Text(metric.label ?? "")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color(.systemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .shadow(color: .black.opacity(0.05), radius: 10, x: 0, y: 5)
                    }
                }
                .padding(.horizontal, 24)
                .offset(y: -30)
                .padding(.bottom, -10)
            }
        }
    }

    // MARK: - Tech Stack

    private func techStackSection(_ tags: [String]) -> some View {
        sectionView(title: "技术栈", icon: "cpu") {
            FlowLayout(spacing: 8) {
                ForEach(tags, id: \.self) { tag in
                    Text(tag)
                        .font(.caption.weight(.semibold))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(Color.accentColor.opacity(0.08))
                        .foregroundColor(.accentColor)
                        .clipShape(Capsule())
                }
            }
        }
    }

    // MARK: - Focus Areas

    private func focusAreasSection(_ areas: [SiteFocusArea]) -> some View {
        sectionView(title: "研究方向", icon: "sparkles") {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(Array(areas.enumerated()), id: \.offset) { index, area in
                    VStack(alignment: .leading, spacing: 10) {
                        Image(systemName: focusIcon(for: area.icon, index: index))
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(width: 36, height: 36)
                            .background(focusColor(index: index))
                            .clipShape(RoundedRectangle(cornerRadius: 10))

                        VStack(alignment: .leading, spacing: 2) {
                            Text(area.title ?? "")
                                .font(.subheadline.bold())
                            
                            if let desc = area.description, !desc.isEmpty {
                                Text(desc)
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                                    .lineLimit(2)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(Color(.systemGroupedBackground).opacity(0.5))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    // MARK: - Activities

    private func activitiesSection(_ activities: [SiteActivity]) -> some View {
        sectionView(title: "近期活动", icon: "calendar") {
            VStack(spacing: 12) {
                ForEach(activities) { activity in
                    NavigationLink {
                        ActivityDetailView(activityId: activity.id)
                    } label: {
                        HStack(spacing: 12) {
                            if let cover = activity.coverUrl, !cover.isEmpty {
                                AsyncImageView(url: cover, size: 64)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            } else {
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color(.systemGray6))
                                    .frame(width: 64, height: 64)
                                    .overlay {
                                        Image(systemName: "calendar")
                                            .foregroundColor(.gray)
                                    }
                            }

                            VStack(alignment: .leading, spacing: 4) {
                                Text(activity.title ?? "")
                                    .font(.subheadline.bold())
                                    .foregroundColor(.primary)
                                
                                HStack(spacing: 8) {
                                    if let date = activity.date {
                                        Label(date, systemImage: "clock")
                                            .font(.caption2)
                                    }
                                    if let location = activity.location {
                                        Label(location, systemImage: "mappin.and.ellipse")
                                            .font(.caption2)
                                    }
                                }
                                .foregroundColor(.secondary)
                            }

                            Spacer()
                            
                            Image(systemName: "chevron.right")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.secondary.opacity(0.5))
                        }
                        .padding(8)
                        .background(Color(.systemGroupedBackground).opacity(0.3))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - People

    private func peopleSection(_ people: [SitePerson]) -> some View {
        sectionView(title: "核心团队", icon: "person.2.fill") {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(people) { person in
                        VStack(spacing: 8) {
                            if let avatar = person.avatar, !avatar.isEmpty {
                                AsyncImageView(url: avatar, size: 56)
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(Color.accentColor.opacity(0.2), lineWidth: 1))
                            } else {
                                Circle()
                                    .fill(LinearGradient(colors: [.accentColor, .blue], startPoint: .top, endPoint: .bottom))
                                    .frame(width: 56, height: 56)
                                    .overlay {
                                        Text(personInitial(person))
                                            .font(.headline)
                                            .foregroundColor(.white)
                                    }
                            }

                            VStack(spacing: 2) {
                                Text(person.name ?? "")
                                    .font(.caption.bold())
                                    .lineLimit(1)
                                if let role = person.role {
                                    Text(role)
                                        .font(.system(size: 10))
                                        .foregroundColor(.secondary)
                                        .lineLimit(1)
                                }
                            }
                        }
                        .frame(width: 80)
                    }
                }
                .padding(.vertical, 4)
            }
        }
    }

    // MARK: - Awards

    private func awardsSection(_ awards: [SiteAward]) -> some View {
        sectionView(title: "荣获奖项", icon: "trophy.fill") {
            VStack(spacing: 12) {
                ForEach(awards.prefix(5)) { award in
                    HStack(spacing: 12) {
                        Text(String(award.year ?? 0))
                            .font(.system(.subheadline, design: .rounded).bold())
                            .foregroundColor(.orange)
                            .frame(width: 44)
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text(award.title ?? "")
                                .font(.subheadline.bold())
                            Text(award.competitionName ?? "")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        if let level = award.awardLevel {
                            Text(level)
                                .font(.system(size: 10, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.orange)
                                .clipShape(Capsule())
                        }
                    }
                    .padding(10)
                    .background(Color(.systemGroupedBackground).opacity(0.5))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    // MARK: - Helpers

    private func personInitial(_ person: SitePerson) -> String {
        if let initial = person.initial, !initial.isEmpty { return initial }
        if let name = person.name, let first = name.first { return String(first) }
        return "?"
    }

    private func sectionView<Content: View>(title: String, icon: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(.accentColor)
                Text(title)
                    .font(.headline)
                Spacer()
            }
            content()
        }
        .padding(16)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.03), radius: 10, x: 0, y: 5)
    }

    private func focusIcon(for apiIcon: String?, index: Int) -> String {
        switch apiIcon {
        case "monitor": return "desktopcomputer"
        case "cpu": return "cpu"
        case "lightning": return "bolt.fill"
        case "phone": return "iphone"
        default:
            let icons = ["desktopcomputer", "cpu", "bolt.fill", "iphone"]
            return icons[index % icons.count]
        }
    }

    private func focusColor(index: Int) -> Color {
        let colors: [Color] = [.blue, .purple, .orange, .green]
        return colors[index % colors.count]
    }
}

// MARK: - Flow Layout

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = arrange(proposal: proposal, subviews: subviews)
        let height = rows.last?.maxY ?? 0
        return CGSize(width: proposal.width ?? 0, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let rows = arrange(proposal: ProposedViewSize(width: bounds.width, height: nil), subviews: subviews)
        for row in rows {
            for item in row.items {
                subviews[item.index].place(
                    at: CGPoint(x: bounds.minX + item.x, y: bounds.minY + row.y),
                    proposal: ProposedViewSize(item.size)
                )
            }
        }
    }

    private struct Row { var items: [(index: Int, x: CGFloat, size: CGSize)]; var y: CGFloat; var maxY: CGFloat }
    private struct Item { let index: Int; let size: CGSize }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> [Row] {
        let maxWidth = proposal.width ?? .infinity
        var rows: [Row] = []
        var currentRow: [(Int, CGFloat, CGSize)] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0

        for (index, subview) in subviews.enumerated() {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0, x + size.width > maxWidth {
                rows.append(Row(items: currentRow, y: y, maxY: y + rowHeight))
                y += rowHeight + spacing
                currentRow = []
                x = 0
                rowHeight = 0
            }
            currentRow.append((index, x, size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }

        if !currentRow.isEmpty {
            rows.append(Row(items: currentRow, y: y, maxY: y + rowHeight))
        }

        return rows
    }
}
