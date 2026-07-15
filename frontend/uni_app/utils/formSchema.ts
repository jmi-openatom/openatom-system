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
  minLength?: number
  maxLength?: number
  options?: Array<FormFieldOption | string>
}

export function parseFormSchema(value?: string | FormField[] | null): FormField[] {
  if (Array.isArray(value)) return normalizeFields(value)
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? normalizeFields(parsed) : []
  } catch {
    return []
  }
}

function normalizeFields(fields: FormField[]): FormField[] {
  return fields
    .filter(Boolean)
    .map((field) => ({...field, key: String(field.key || field.label || '').trim()}))
    .filter((field) => Boolean(field.key))
}

export function optionLabel(option: FormFieldOption | string): string {
  return typeof option === 'string' ? option : String(option.label || option.value || '')
}

export function optionValue(option: FormFieldOption | string): string | number {
  return typeof option === 'string' ? option : option.value ?? option.label ?? ''
}

export function normalizeFieldType(field: FormField): string {
  return String(field.type || 'text').toLowerCase()
}

export function isOptionField(field: FormField): boolean {
  return ['select', 'dropdown', 'radio'].includes(normalizeFieldType(field))
}

export function fieldInputType(field: FormField): 'text' | 'number' | 'digit' {
  const type = normalizeFieldType(field)
  if (type === 'phone' || type === 'tel') return 'number'
  if (type === 'number') return 'digit'
  return 'text'
}

export function validateField(field: FormField, rawValue: unknown): string {
  const value = String(rawValue ?? '').trim()
  const label = field.label || field.key
  const type = normalizeFieldType(field)
  if (field.required && !value) return `请填写${label}`
  if (!value) return ''
  if (['phone', 'tel'].includes(type) && !/^1[3-9]\d{9}$/.test(value)) return `${label}格式不正确`
  if (type === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return `${label}格式不正确`
  if (type === 'number' && !Number.isFinite(Number(value))) return `${label}必须是数字`
  if (field.minLength && value.length < field.minLength) return `${label}至少填写 ${field.minLength} 个字符`
  if (field.maxLength && value.length > field.maxLength) return `${label}最多填写 ${field.maxLength} 个字符`
  return ''
}
