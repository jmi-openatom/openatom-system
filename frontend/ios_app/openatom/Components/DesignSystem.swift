import SwiftUI

struct AppTheme {
    static let primary = Color.accentColor
    static let secondary = Color(.secondaryLabel)
    static let background = Color(.systemGroupedBackground)
    static let cardBackground = Color(.systemBackground)
    
    static let cornerRadius: CGFloat = 16
    static let cardPadding: CGFloat = 16
    
}

extension View {
    func premiumCard() -> some View {
        self
            .padding(AppTheme.cardPadding)
            .background(AppTheme.cardBackground)
            .clipShape(RoundedRectangle(cornerRadius: AppTheme.cornerRadius))
            .shadow(color: Color.black.opacity(0.05), radius: 10, x: 0, y: 4)
    }
    
    func sectionTitle() -> some View {
        self
            .font(.headline)
            .foregroundColor(.primary)
            .padding(.bottom, 8)
    }
}

struct ModernHeader: View {
    let title: String
    let subtitle: String?
    let systemImage: String?
    
    var body: some View {
        VStack(spacing: 12) {
            if let systemImage = systemImage {
                Image(systemName: systemImage)
                    .font(.system(size: 40))
                    .foregroundColor(.white)
                    .frame(width: 80, height: 80)
                    .background(
                        LinearGradient(
                            colors: [AppTheme.primary, AppTheme.primary.opacity(0.7)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .clipShape(Circle())
                    .shadow(color: AppTheme.primary.opacity(0.3), radius: 10, x: 0, y: 5)
            }
            
            Text(title)
                .font(.title.bold())
            
            if let subtitle = subtitle {
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
    }
}
