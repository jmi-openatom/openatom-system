export interface ClubHomeClub {
  id?: number
  name?: string
  code?: string
  category?: string
  description?: string
  logoUrl?: string
  recruitmentStatus?: string
}

export interface ClubHomeMetric {
  label?: string
  value?: string
  note?: string
}

export interface ClubHomeFocusArea {
  title?: string
  description?: string
  icon?: string
}

export interface ClubHomeActivity {
  id?: number
  date?: string
  title?: string
  description?: string
  location?: string
  status?: string
  coverUrl?: string
}

export interface ClubHomePerson {
  userId?: number
  name?: string
  initial?: string
  role?: string
  focus?: string
  avatar?: string
}

export interface ClubHomeAward {
  id?: number
  year?: number
  title?: string
  competitionName?: string
  awardLevel?: string
  teamName?: string
  description?: string
}

export interface ClubHomePayload {
  club?: ClubHomeClub
  metrics?: ClubHomeMetric[]
  focusAreas?: ClubHomeFocusArea[]
  activities?: ClubHomeActivity[]
  people?: ClubHomePerson[]
  awards?: ClubHomeAward[]
  techStack?: string[]
}
