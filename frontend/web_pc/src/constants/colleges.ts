export const COLLEGE_NAMES = [
  '航海技术学院',
  '轮机与电气工程学院',
  '船舶与智能制造学院',
  '经济管理学院',
  '信息工程学院',
  '邮轮与艺术设计学院',
  '国防教育与定向培养学院',
  '创新创业学院',
  '国际教育学院',
] as const

export const COLLEGE_FIELD_KEY = 'college'

export function createCollegeFormField() {
  return {
    key: COLLEGE_FIELD_KEY,
    label: '学院',
    type: 'select',
    required: true,
    placeholder: '请选择学院',
    options: COLLEGE_NAMES.map((college) => ({ label: college, value: college })),
  }
}

export function ensureCollegeFormField(value: unknown) {
  const fields = Array.isArray(value) ? value.filter((item) => item && typeof item === 'object') : []
  const existingIndex = fields.findIndex((field) => {
    const key = String(field.key || '')
      .trim()
      .toLowerCase()
    const label = String(field.label || '').trim()
    return key === COLLEGE_FIELD_KEY || label === '学院'
  })
  const collegeField = createCollegeFormField()

  if (existingIndex >= 0) {
    return fields.map((field, index) =>
      index === existingIndex
        ? {
            ...field,
            ...collegeField,
          }
        : field,
    )
  }

  const studentIdIndex = fields.findIndex((field) => String(field.key || '').trim() === 'studentId')
  const insertIndex = studentIdIndex >= 0 ? studentIdIndex + 1 : Math.min(1, fields.length)
  return [...fields.slice(0, insertIndex), collegeField, ...fields.slice(insertIndex)]
}

export function createCollegeRegistrationField() {
  return {
    label: '学院',
    type: 'select',
    required: true,
    options: [...COLLEGE_NAMES],
  }
}

export function ensureCollegeRegistrationField(value: unknown) {
  const fields = Array.isArray(value) ? value.filter((item) => item && typeof item === 'object') : []
  const existingIndex = fields.findIndex((field) => {
    const key = String(field.key || '')
      .trim()
      .toLowerCase()
    return key === COLLEGE_FIELD_KEY || String(field.label || '').trim() === '学院'
  })
  const collegeField = createCollegeRegistrationField()

  if (existingIndex >= 0) {
    return fields.map((field, index) =>
      index === existingIndex
        ? {
            ...field,
            ...collegeField,
          }
        : field,
    )
  }

  const nameIndex = fields.findIndex((field) => String(field.label || '').trim() === '姓名')
  const insertIndex = nameIndex >= 0 ? nameIndex + 1 : 0
  return [...fields.slice(0, insertIndex), collegeField, ...fields.slice(insertIndex)]
}

export function resolveCollegeValue(value: unknown) {
  const college = String(value || '').trim()
  return COLLEGE_NAMES.includes(college as (typeof COLLEGE_NAMES)[number]) ? college : ''
}
