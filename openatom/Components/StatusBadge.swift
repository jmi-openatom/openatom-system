import SwiftUI

struct StatusBadge: View {
    let status: String?

    var body: some View {
        Text(statusText)
            .font(.caption2)
            .fontWeight(.medium)
            .foregroundColor(statusColor)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(statusColor.opacity(0.12))
            .clipShape(Capsule())
    }

    private var statusText: String {
        switch status {
        case "ACTIVE": return "正常"
        case "DISABLED": return "停用"
        case "LOCKED": return "锁定"
        case "DRAFT": return "草稿"
        case "SUBMITTED", "submitted": return "已提交"
        case "PENDING", "pending": return "待审核"
        case "APPROVED", "approved": return "已通过"
        case "REJECTED", "rejected": return "已拒绝"
        case "PUBLISHED": return "已发布"
        case "CLOSED": return "已关闭"
        case "OPEN": return "进行中"
        case "ACCEPTED": return "已录取"
        case "INTERVIEWING": return "面试中"
        case "SCHEDULED": return "已安排"
        case "COMPLETED": return "已完成"
        case "WITHDRAWN": return "已撤回"
        default: return status ?? "未知"
        }
    }

    private var statusColor: Color {
        switch status {
        case "ACTIVE", "APPROVED", "approved", "PUBLISHED", "ACCEPTED", "COMPLETED": return .green
        case "DISABLED", "LOCKED", "REJECTED", "rejected", "CLOSED": return .red
        case "PENDING", "pending", "SUBMITTED", "submitted", "INTERVIEWING", "SCHEDULED", "DRAFT": return .orange
        case "WITHDRAWN": return .gray
        case "OPEN": return .blue
        default: return .secondary
        }
    }
}
