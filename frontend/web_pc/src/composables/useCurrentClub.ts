import { computed, ref } from 'vue'
import { authApi } from '@/api'

const STORAGE_KEY = 'openatom_current_club_id'

const clubs = ref<any[]>([])
const loading = ref(false)
const currentClubId = ref<string | number | ''>(localStorage.getItem(STORAGE_KEY) || '')

const manageableClubs = computed(() => clubs.value.filter((item) => item.manageable))

const currentClub = computed(() => {
  return manageableClubs.value.find((item) => String(item.clubId) === String(currentClubId.value)) || null
})

async function loadCurrentClubs() {
  loading.value = true
  try {
    const result = await authApi.myClubs()
    clubs.value = Array.isArray(result) ? result : []
    if (!manageableClubs.value.some((item) => String(item.clubId) === String(currentClubId.value))) {
      currentClubId.value = manageableClubs.value[0]?.clubId || ''
      persistCurrentClub()
    }
  } finally {
    loading.value = false
  }
}

function setCurrentClub(clubId: string | number | '') {
  currentClubId.value = clubId
  persistCurrentClub()
}

function persistCurrentClub() {
  if (currentClubId.value) localStorage.setItem(STORAGE_KEY, String(currentClubId.value))
  else localStorage.removeItem(STORAGE_KEY)
}

export function useCurrentClub() {
  return {
    clubs,
    manageableClubs,
    currentClub,
    currentClubId,
    loading,
    loadCurrentClubs,
    setCurrentClub,
  }
}
