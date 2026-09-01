import { defineStore } from 'pinia'

/**
 * Application-level store skeleton (TASK-001).
 * Only site identity lives here for now; auth (TASK-003) and theme (TASK-004)
 * stores are added by their respective vertical slices.
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    name: '星雨笔录',
    englishName: 'Star Rain Notes',
    tagline: '建立自己的知识世界',
  }),
})
