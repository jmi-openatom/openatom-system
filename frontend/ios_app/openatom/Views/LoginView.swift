import SwiftUI

struct LoginView: View {
    @Environment(Session.self) private var session
    @State private var vm = AuthViewModel()

    @State private var username = ""
    @State private var password = ""
    @State private var isRegistering = false
    @State private var realName = ""
    @State private var phone = ""
    @State private var email = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                logoSection
                formSection
            }
            .padding(24)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle(isRegistering ? "注册" : "登录")
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.checkRegisterEnabled() }
    }

    private var logoSection: some View {
        VStack(spacing: 12) {
            Image(systemName: "atom")
                .font(.system(size: 56))
                .foregroundColor(.accentColor)
                .padding(.top, 32)
            Text("JMI-OPENATOM")
                .font(.title.bold())
            Text("社团管理系统")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }

    private var formSection: some View {
        VStack(spacing: 16) {
            if isRegistering {
                TextField("用户名", text: $username)
                    .textContentType(.username)
                    .textFieldStyle(.roundedBorder)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()

                TextField("真实姓名", text: $realName)
                    .textContentType(.name)
                    .textFieldStyle(.roundedBorder)

                TextField("手机号", text: $phone)
                    .textContentType(.telephoneNumber)
                    .keyboardType(.phonePad)
                    .textFieldStyle(.roundedBorder)

                TextField("邮箱", text: $email)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .textFieldStyle(.roundedBorder)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()

                SecureField("密码", text: $password)
                    .textContentType(.newPassword)
                    .textFieldStyle(.roundedBorder)
            } else {
                TextField("用户名", text: $username)
                    .textContentType(.username)
                    .textFieldStyle(.roundedBorder)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()

                SecureField("密码", text: $password)
                    .textContentType(.password)
                    .textFieldStyle(.roundedBorder)
            }

            if let error = vm.error {
                Text(error)
                    .font(.callout)
                    .foregroundColor(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Button(action: submitForm) {
                if vm.isLoading {
                    ProgressView()
                        .tint(.white)
                } else {
                    Text(isRegistering ? "注册" : "登录")
                        .fontWeight(.semibold)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(Color.accentColor)
            .foregroundColor(.white)
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .disabled(vm.isLoading)

            if vm.registerEnabled {
                Button(isRegistering ? "已有账号？去登录" : "没有账号？去注册") {
                    isRegistering.toggle()
                    vm.error = nil
                }
                .font(.subheadline)
            }
        }
        .padding(20)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.05), radius: 8, y: 2)
    }

    private func submitForm() {
        Task {
            if isRegistering {
                await vm.register(
                    userName: username,
                    realName: realName,
                    phone: phone,
                    email: email,
                    password: password
                )
            } else {
                await vm.login(username: username, password: password)
            }
        }
    }
}
