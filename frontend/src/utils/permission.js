import { getCurrentPermissions, getCurrentRoles, getToken } from './auth'

const adminRoles = ['super_admin', 'club_admin', 'department_head']

export function hasRole(roleCode) {
  return getCurrentRoles().includes(roleCode)
}

export function hasPermission(permissionCode) {
  if (!permissionCode) return true
  if (hasRole('super_admin')) return true
  return getCurrentPermissions().includes(permissionCode)
}

export function hasAnyPermission(permissionCodes = []) {
  if (!permissionCodes || !permissionCodes.length) return true
  if (!getToken()) return false
  if (hasRole('super_admin')) return true
  return permissionCodes.some((code) => getCurrentPermissions().includes(code))
}

export function hasAdminAccess() {
  if (!getToken()) return false
  return adminRoles.some((role) => hasRole(role))
}
