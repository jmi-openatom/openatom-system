import Foundation

struct ResponseRecruitmentDTO: Codable {
    let club: Club?
    let campaigns: [RecruitmentCampaign]?
    let departments: [ClubDepartment]?
}

struct ResponseRecruitmentDetailDTO: Codable {
    let club: Club?
    let campaign: RecruitmentCampaign?
    let departments: [ClubDepartment]?
}

struct RecruitmentCampaign: Codable, Identifiable {
    let id: Int
    let clubId: Int?
    let name: String?
    let applyStartAt: String?
    let applyEndAt: String?
    let interviewStartAt: String?
    let interviewEndAt: String?
    let resultPublishAt: String?
    let targetGrades: String?
    let maxApplicants: Int?
    let loginRequired: Bool?
    let formSchema: String?
    let status: String?
    let createdAt: String?
    let updatedAt: String?

    var parsedFormSchema: FormSchema? {
        guard let json = formSchema, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(FormSchema.self, from: data)
    }
}

struct FormSchema: Codable {
    let fields: [FormField]?
}

struct FormField: Codable, Identifiable {
    let key: String?
    let label: String?
    let type: String?
    let required: Bool?
    let options: [FormFieldOption]?
    let placeholder: String?

    var id: String { key ?? UUID().uuidString }
}

struct FormFieldOption: Codable, Identifiable {
    var id: String { value ?? UUID().uuidString }
    let label: String?
    let value: String?
}
