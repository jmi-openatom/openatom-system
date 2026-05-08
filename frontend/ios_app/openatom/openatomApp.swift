import SwiftUI

@main
struct openatomApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(Session.shared)
        }
    }
}
