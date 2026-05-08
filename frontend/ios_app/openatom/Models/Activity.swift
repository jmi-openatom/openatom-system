import Foundation

struct Activity: Codable, Identifiable {
    let id: Int
    let clubId: Int?
    let title: String?
    let summary: String?
    let descriptionMarkdown: String?
    let activityAt: Double?
    let endAt: Double?
    let location: String?
    let status: String?
    let coverUrl: String?
    let registrationRequired: Bool?
    let registrationStartAt: Double?
    let registrationEndAt: Double?
    let registrationFields: String?
    let createdAt: Double?
    let updatedAt: Double?

    var formattedDate: String {
        guard let ts = activityAt, ts > 0 else { return "" }
        let date = Date(timeIntervalSince1970: ts / 1000)
        let fmt = DateFormatter()
        fmt.dateFormat = "MM.dd HH:mm"
        return fmt.string(from: date)
    }

    var formattedDateRange: String {
        guard let start = activityAt, start > 0 else { return "" }
        let startDate = Date(timeIntervalSince1970: start / 1000)
        let fmt = DateFormatter()
        fmt.dateFormat = "MM.dd HH:mm"
        var result = fmt.string(from: startDate)
        if let end = endAt, end > 0 {
            let endDate = Date(timeIntervalSince1970: end / 1000)
            result += " - \(fmt.string(from: endDate))"
        }
        return result
    }
}

struct ActivityRegistration: Codable, Identifiable {
    let id: Int?
    let activityId: Int?
    let userId: Int?
    let formData: String?
    let status: String?
}
