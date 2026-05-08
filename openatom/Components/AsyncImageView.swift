import SwiftUI

struct AsyncImageView: View {
    let url: String?
    var size: CGFloat = 48

    @State private var image: UIImage?
    @State private var isLoading = false

    var body: some View {
        Group {
            if let image = image {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } else {
                ZStack {
                    Color(.systemGray5)
                    if isLoading {
                        ProgressView()
                    } else {
                        Image(systemName: "photo")
                            .foregroundColor(.gray)
                    }
                }
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .task {
            await loadImage()
        }
    }

    private func loadImage() async {
        guard let url = url, let urlObj = URL(string: url) else { return }
        isLoading = true
        defer { isLoading = false }
        do {
            let (data, _) = try await URLSession.shared.data(from: urlObj)
            image = UIImage(data: data)
        } catch {}
    }
}

struct AvatarView: View {
    let url: String?
    let name: String
    var size: CGFloat = 40

    var body: some View {
        Group {
            if let url = url, !url.isEmpty {
                AsyncImageView(url: url, size: size)
            } else {
                ZStack {
                    Circle()
                        .fill(Color.accentColor.opacity(0.2))
                    Text(String(name.prefix(1)))
                        .font(.system(size: size * 0.45, weight: .semibold))
                        .foregroundColor(.accentColor)
                }
                .frame(width: size, height: size)
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
    }
}
