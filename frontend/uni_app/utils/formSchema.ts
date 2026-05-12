export interface FormFieldOption {
  label?: string
  value?: string | number
}

export interface FormField {
  key: string
  label?: string
  type?: string
  placeholder?: string
  required?: boolean
  options?: Array<FormFieldOption | string>
}

export function parseFormSchema(value?: string | FormField[] | null): FormField[] {
  if (Array.isArray(value)) return value.filter((item) => item && item.key)
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.filter((item) => item && item.key) : []
  } catch {
    return []
  }
}

export function optionLabel(option: FormFieldOption | string): string {
  return typeof option === 'string' ? option : String(option.label || option.value || '')
}

export function optionValue(option: FormFieldOption | string): string | number {
  return typeof option === 'string' ? option : option.value ?? option.label ?? ''
}
