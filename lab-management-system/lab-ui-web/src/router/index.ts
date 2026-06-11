import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Problem from '../views/Problem.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home
    },
    {
      path: '/problem',
      name: 'problem',
      component: Problem
    }
  ]
})

export default router
