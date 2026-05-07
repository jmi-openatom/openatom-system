import { getCurrentPermissions, getCurrentRoles, getToken } from './auth'

const adminRoles = ['super_admin', 'club_admin', 'department_head']

export function hasRole(roleCode: string): boolean {
  return getCurrentRoles().includes(roleCode)
}

export function hasPermission(permissionCode: string): boolean {
  if (!permissionCode) return true
  if (hasRole('super_admin')) return true
  return getCurrentPermissions().includes(permissionCode)
}

export function hasAnyPermission(permissionCodes: string[] = []): boolean {
  if (!permissionCodes || !permissionCodes.length) return true
  if (!getToken()) return false
  if (hasRole('super_admin')) return true
  return permissionCodes.some((code) => getCurrentPermissions().includes(code))
}

export function hasAdminAccess(): boolean {
  if (!getToken()) return false
  return adminRoles.some((role) => hasRole(role))
}
