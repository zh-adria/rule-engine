import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const currentPage = ref('dashboard')
  const sidebarCollapsed = ref(false)
  const loading = ref(false)

  // Version diff dialog state
  const diffDialogVisible = ref(false)
  const diffOldVersion = ref(null)
  const diffNewVersion = ref(null)
  const diffHunks = ref([])
  const diffStat = ref({ added: 0, removed: 0, unchanged: 0 })

  function setCurrentPage(page) {
    currentPage.value = page
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function showDiff(oldVersion, newVersion, hunks, stat) {
    diffOldVersion.value = oldVersion
    diffNewVersion.value = newVersion
    diffHunks.value = hunks
    diffStat.value = stat
    diffDialogVisible.value = true
  }

  function hideDiff() {
    diffDialogVisible.value = false
    diffOldVersion.value = null
    diffNewVersion.value = null
    diffHunks.value = []
    diffStat.value = { added: 0, removed: 0, unchanged: 0 }
  }

  return {
    currentPage,
    sidebarCollapsed,
    loading,
    diffDialogVisible,
    diffOldVersion,
    diffNewVersion,
    diffHunks,
    diffStat,
    setCurrentPage,
    toggleSidebar,
    showDiff,
    hideDiff
  }
})
