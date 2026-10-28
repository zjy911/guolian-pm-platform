import { reactive, ref, watch, type Ref } from 'vue'
import { useAppStore } from '@/stores/app'
import type { PageResult } from '@/api/types'

/**
 * CRM 列表页通用逻辑：分页 / 加载态 / 空态 / 错误提示 / 条件重置。
 * 9 个对象共用同一套交互，没必要各写一遍。
 *
 * 查询对象用 Record<string, any>：字段随对象差异很大，且需要在模板里直接 v-model 绑定。
 */
export function useCrmList<T>(
  fetcher: (params: Record<string, any>) => Promise<PageResult<T>>,
  defaults: Record<string, any> = {}
) {
  const appStore = useAppStore()
  const list = ref([]) as Ref<T[]>
  const total = ref(0)
  const loading = ref(false)
  const loaded = ref(false)
  const pageNum = ref(1)
  const pageSize = ref(20)
  const query = reactive<Record<string, any>>({ ...defaults })

  async function load() {
    loading.value = true
    try {
      const params: Record<string, any> = { pageNum: pageNum.value, pageSize: pageSize.value }
      Object.entries(query).forEach(([key, value]) => {
        if (value === '' || value === null || value === undefined) return
        params[key] = value
      })
      const data = await fetcher(params)
      list.value = data.list ?? []
      total.value = data.total ?? 0
      loaded.value = true
    } catch (error) {
      appStore.toast(appStore.errorMessage(error), 'error')
    } finally {
      loading.value = false
    }
  }

  function reset() {
    Object.keys(query).forEach((key) => {
      query[key] = defaults[key] ?? ''
    })
    pageNum.value = 1
    load()
  }

  watch(pageNum, load)
  watch(pageSize, () => {
    pageNum.value = 1
    load()
  })

  return { list, total, loading, loaded, pageNum, pageSize, query, load, reset }
}
