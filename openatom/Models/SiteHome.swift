import Foundation

struct SiteHome: Codable {
    let club: SiteClub?
    let metrics: [SiteMetric]?
    let focusAreas: [SiteFocusArea]?
    let activities: [SiteActivity]?
    let people: [SitePerson]?
    let awards: [SiteAward]?
    let techStack: [String]?
}

struct SiteClub: Codable {
    let id: Int?
    let name: String?
    let code: String?
    let category: String?
    let description: String?
    let logoUrl: String?
    let recruitmentStatus: String?
}

struct SiteMetric: Codable {
    let label: String?
    let value: String?
    let note: String?
}

struct SiteFocusArea: Codable {
    let title: String?
    let description: String?
    let icon: String?
}

struct SiteActivity: Codable, Identifiable {
    let id: Int
    let date: String?
    let title: String?
    let description: String?
    let location: String?
    let status: String?
    let coverUrl: String?
}

struct SitePerson: Codable, Identifiable {
    let userId: Int?
    let name: String?
    let initial: String?
    let role: String?
    let focus: String?
    let avatar: String?

    var id: Int { userId ?? 0 }
}

struct SiteAward: Codable, Identifiable {
    let id: Int
    let year: Int?
    let title: String?
    let competitionName: String?
    let awardLevel: String?
    let teamName: String?
    let description: String?
}
