import SwiftUI

struct RecruitmentView: View {
    @Environment(Session.self) private var session
    @State private var vm = RecruitmentViewModel()

    var body: some View {
        NavigationStack {
            Group {
                if vm.isLoading {
                    LoadingView("加载中...")
                } else if let error = vm.error {
                    ErrorView(message: error) { Task { await vm.load() } }
                } else if vm.campaigns.isEmpty {
                    EmptyStateView(icon: "person.badge.plus", title: "暂无开放招募")
                } else {
                    listView
                }
            }
            .navigationTitle("入会申请")
            .navigationBarTitleDisplayMode(.large)
            .refreshable { await vm.load() }
            .task { await vm.load() }
        }
    }

    private var listView: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(vm.campaigns) { campaign in
                    NavigationLink {
                        ApplicationFormView(campaignId: campaign.id)
                    } label: {
                        CampaignRow(campaign: campaign, clubName: vm.club?.name)
                            .premiumCard()
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
        }
    }
}

struct CampaignRow: View {
    let campaign: RecruitmentCampaign
    let clubName: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(campaign.name ?? "招募")
                    .font(.headline)
                Spacer()
                if let status = campaign.status {
                    StatusBadge(status: status)
                }
            }

            if let name = clubName {
                Label(name, systemImage: "building.2")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            HStack(spacing: 16) {
                if let start = campaign.applyStartAt {
                    Label("开始: \(start)", systemImage: "calendar")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                if let end = campaign.applyEndAt {
                    Label("截止: \(end)", systemImage: "clock")
                        .font(.caption)
                        .foregroundColor(.red.opacity(0.8))
                }
            }

            if let grades = campaign.targetGrades, !grades.isEmpty {
                Text("面向: \(grades)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

struct ApplicationFormView: View {
    let campaignId: Int
    @Environment(\.dismiss) private var dismiss
    @Environment(Session.self) private var session
    @State private var vm = ApplicationFormViewModel()

    var body: some View {
        Group {
            if vm.isLoading {
                LoadingView("加载中...")
            } else if let error = vm.error {
                ErrorView(message: error) { Task { await vm.loadCampaign(id: campaignId) } }
            } else if let campaign = vm.campaign {
                formContent(campaign)
            }
        }
        .navigationTitle("填写申请")
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.loadCampaign(id: campaignId) }
        .alert("提交成功", isPresented: $vm.submitSuccess) {
            Button("确定") { dismiss() }
        } message: {
            Text("您的申请已提交，请等待审核")
        }
    }

    private func formContent(_ campaign: RecruitmentCampaign) -> some View {
        ScrollView {
            VStack(spacing: 20) {
                campaignInfoCard(campaign)

                if let schema = campaign.parsedFormSchema, let fields = schema.fields, !fields.isEmpty {
                    formFieldsSection(fields)
                } else {
                    defaultFormSection()
                }

                if !session.isLoggedIn {
                    Text("请先登录后再提交申请")
                        .font(.caption)
                        .foregroundColor(.red)
                }

                Button(action: { Task { await vm.submit() } }) {
                    Text("提交申请")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!session.isLoggedIn)
                .padding(.horizontal)
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
    }

    private func campaignInfoCard(_ campaign: RecruitmentCampaign) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(campaign.name ?? "招募详情")
                .font(.headline)
            if let clubName = vm.club?.name {
                Text(clubName)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            if let end = campaign.applyEndAt {
                Text("截止日期: \(end)")
                    .font(.caption)
                    .foregroundColor(.red.opacity(0.8))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func formFieldsSection(_ fields: [FormField]) -> some View {
        VStack(spacing: 12) {
            ForEach(fields) { field in
                formFieldView(field)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func defaultFormSection() -> some View {
        VStack(spacing: 12) {
            Text("申请信息")
                .font(.headline)
                .frame(maxWidth: .infinity, alignment: .leading)

            TextField("自我介绍 / 申请理由", text: binding(for: "reason"), axis: .vertical)
                .textFieldStyle(.roundedBorder)
                .lineLimit(4...8)
        }
        .padding()
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func formFieldView(_ field: FormField) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(field.label ?? field.key ?? "")
                    .font(.subheadline.weight(.medium))
                if field.required == true {
                    Text("*")
                        .foregroundColor(.red)
                }
            }

            switch field.type {
            case "select":
                Picker(field.label ?? "", selection: binding(for: field.key ?? "")) {
                    Text("请选择").tag("")
                    if let options = field.options {
                        ForEach(options) { opt in
                            Text(opt.label ?? opt.value ?? "").tag(opt.value ?? "")
                        }
                    }
                }
                .pickerStyle(.menu)

            case "textarea":
                TextField(field.placeholder ?? field.label ?? "", text: binding(for: field.key ?? ""), axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .lineLimit(3...6)

            default:
                TextField(field.placeholder ?? field.label ?? "", text: binding(for: field.key ?? ""))
                    .textFieldStyle(.roundedBorder)
            }
        }
    }

    private func binding(for key: String) -> Binding<String> {
        Binding(
            get: { vm.formData[key] ?? "" },
            set: { vm.formData[key] = $0 }
        )
    }
}
