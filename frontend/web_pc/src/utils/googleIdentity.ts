export type GoogleCredentialResponse = { credential?: string }

export const googleClientId =
  import.meta.env.VITE_GOOGLE_CLIENT_ID ||
  '456373207373-4md7t52dkhbo7p48e8omhjoct2squ06c.apps.googleusercontent.com'

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize(options: {
            client_id: string
            callback: (response: GoogleCredentialResponse) => void
          }): void
          renderButton(
            element: HTMLElement,
            options: Record<string, string | number | boolean>,
          ): void
          cancel(): void
        }
      }
    }
  }
}

export function loadGoogleIdentityServices(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>('script[data-google-identity]')
    const script = existing || document.createElement('script')
    script.addEventListener('load', () => resolve(), { once: true })
    script.addEventListener('error', () => reject(new Error('Google Identity Services加载失败')), {
      once: true,
    })
    if (!existing) {
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.defer = true
      script.dataset.googleIdentity = 'true'
      document.head.appendChild(script)
    }
  })
}

export async function renderGoogleButton(
  element: HTMLElement,
  callback: (response: GoogleCredentialResponse) => void,
  text: 'signin_with' | 'continue_with' = 'signin_with',
) {
  await loadGoogleIdentityServices()
  if (!window.google?.accounts?.id) throw new Error('Google Identity Services不可用')
  window.google.accounts.id.initialize({ client_id: googleClientId, callback })
  window.google.accounts.id.renderButton(element, {
    type: 'standard',
    theme: 'outline',
    size: 'large',
    shape: 'pill',
    text,
    logo_alignment: 'left',
    width: Math.max(240, Math.floor(element.clientWidth)),
  })
}

export function cancelGoogleIdentityPrompt() {
  window.google?.accounts?.id.cancel()
}
