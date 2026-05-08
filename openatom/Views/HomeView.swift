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
            VStack(spacing: 24) {
                if let club = home.club {
                    heroSection(club: club, metrics: home.metrics ?? [])
                }

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
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
    }

    // MARK: - Hero

    private func heroSection(club: SiteClub, metrics: [SiteMetric]) -> some View {
        VStack(spacing: 20) {
            VStack(spacing: 12) {
                if let logo = club.logoUrl, !logo.isEmpty {
                    AsyncImageView(url: logo, size: 80)
                        .clipShape(RoundedRectangle(cornerRadius: 20))
                } else {
                    Image(systemName: "atom")
                        .font(.system(size: 44))
                        .foregroundColor(.accentColor)
                        .frame(width: 80, height: 80)
                        .background(Color.accentColor.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 20))
                }

                Text(club.name ?? "社团")
                    .font(.largeTitle.bold())

                if let desc = club.description, !desc.isEmpty {
                    Text(desc)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .lineLimit(3)
                }

                if let status = club.recruitmentStatus {
                    StatusBadge(status: status)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 24)
            .padding(.horizontal, 16)
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 20))

            if !metrics.isEmpty {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(Array(metrics.enumerated()), id: \.offset) { _, metric in
                        VStack(spacing: 6) {
                            Text(metric.value ?? "0")
                                .font(.title.bold())
                                .foregroundColor(.accentColor)
                            Text(metric.label ?? "")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            if let note = metric.note {
                                Text(note)
                                    .font(.caption2)
                                    .foregroundColor(.secondary.opacity(0.7))
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(Color(.systemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
            }
        }
    }

    // MARK: - Tech Stack

    private func techStackSection(_ tags: [String]) -> some View {
        sectionView(title: "技术栈", icon: "cpu") {
            FlowLayout(spacing: 8) {
                ForEach(tags, id: \.self) { tag in
                    Text(tag)
                        .font(.caption.weight(.medium))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.accentColor.opacity(0.1))
                        .foregroundColor(.accentColor)
                        .clipShape(Capsule())
                }
            }
        }
    }

    // MARK: - Focus Areas

    private func focusAreasSection(_ areas: [SiteFocusArea]) -> some View {
        sectionView(title: "方向简介", icon: "square.grid.2x2") {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(Array(areas.enumerated()), id: \.offset) { index, area in
                    VStack(spacing: 8) {
                        Image(systemName: focusIcon(for: area.icon, index: index))
                            .font(.title2)
                            .foregroundColor(focusColor(index: index))
                            .frame(width: 40, height: 40)
                            .background(focusColor(index: index).opacity(0.1))
                            .clipShape(RoundedRectangle(cornerRadius: 10))

                        Text(area.title ?? "")
                            .font(.subheadline.weight(.semibold))

                        if let desc = area.description, !desc.isEmpty {
                            Text(desc)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(2)
                                .multilineTextAlignment(.center)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(12)
                    .background(Color(.systemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    // MARK: - Activities

    private func activitiesSection(_ activities: [SiteActivity]) -> some View {
        sectionView(title: "近期活动", icon: "calendar") {
            ForEach(activities) { activity in
                NavigationLink {
                    ActivityDetailView(activityId: activity.id)
                } label: {
                    HStack(spacing: 12) {
                        if let cover = activity.coverUrl, !cover.isEmpty {
                            AsyncImageView(url: cover, size: 60)
                                .clipShape(RoundedRectangle(cornerRadius: 8))
                        } else {
                            RoundedRectangle(cornerRadius: 8)
                                .fill(Color(.systemGray5))
                                .frame(width: 60, height: 60)
                                .overlay {
                                    Image(systemName: "calendar")
                                        .foregroundColor(.gray)
                                }
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            Text(activity.title ?? "")
                                .font(.subheadline.weight(.medium))
                                .lineLimit(2)
                                .foregroundColor(.primary)
                            if let desc = activity.description, !desc.isEmpty {
                                Text(desc)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .lineLimit(2)
                            }
                            HStack(spacing: 8) {
                                if let date = activity.date {
                                    Label(date, systemImage: "calendar")
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                }
                                if let location = activity.location {
                                    Label(location, systemImage: "location")
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                }
                            }
                        }

                        Spacer()

                        VStack(spacing: 4) {
                            if let status = activity.status {
                                StatusBadge(status: status)
                            }
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .buttonStyle(.plain)

                if activity.id != activities.last?.id {
                    Divider()
                }
            }
        }
    }

    // MARK: - People

    private func peopleSection(_ people: [SitePerson]) -> some View {
        sectionView(title: "核心成员 (\(people.count))", icon: "person.3") {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(people) { person in
                    VStack(spacing: 8) {
                        if let avatar = person.avatar, !avatar.isEmpty {
                            AsyncImageView(url: avatar, size: 48)
                                .clipShape(Circle())
                        } else {
                            Circle()
                                .fill(
                                    LinearGradient(
                                        colors: [.accentColor, .accentColor.opacity(0.6)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                                .frame(width: 48, height: 48)
                                .overlay {
                                    Text(personInitial(person))
                                        .font(.headline)
                                        .foregroundColor(.white)
                                }
                        }

                        Text(person.name ?? "")
                            .font(.subheadline.weight(.medium))
                            .lineLimit(1)

                        if let role = person.role {
                            Text(role)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color(.systemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    // MARK: - Awards

    private func awardsSection(_ awards: [SiteAward]) -> some View {
        sectionView(title: "获奖荣誉 (\(awards.count))", icon: "trophy") {
            ForEach(awards) { award in
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        if let year = award.year {
                            Text(String(year))
                                .font(.title3.bold())
                                .foregroundColor(.accentColor)
                        }
                        Text(award.title ?? "")
                            .font(.subheadline.weight(.medium))
                        Spacer()
                        if let level = award.awardLevel {
                            Text(level)
                                .font(.caption.weight(.medium))
                                .foregroundColor(.orange)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(Color.orange.opacity(0.1))
                                .clipShape(Capsule())
                        }
                    }

                    if let comp = award.competitionName {
                        Text(comp)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    if let team = award.teamName {
                        Text(team)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.vertical, 4)

                if award.id != awards.last?.id {
                    Divider()
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
        VStack(alignment: .leading, spacing: 12) {
            Label(title, systemImage: icon)
                .font(.headline)
            content()
        }
        .padding(16)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func focusIcon(for apiIcon: String?, index: Int) -> String {
        switch apiIcon {
        case "monitor": return "desktopcomputer"
        case "cpu": return "cpu"
        case "lightning": return "bolt"
        case "phone": return "iphone"
        default:
            let icons = ["desktopcomputer", "cpu", "bolt", "iphone"]
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
