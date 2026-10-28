import { reactive } from 'vue'
import { dictApi } from '@/api'
import type { DictItem } from '@/api/types'

/**
 * CRM 字典加载：一次拉齐页面用到的字典，之后按 (type, value) 取文案。
 * 字典加载失败不阻塞页面（下拉为空、标签回退成原值），避免一个字典挂掉整页不可用。
 */
export function useCrmDict(types: string[]) {
  const map = reactive<Record<string, DictItem[]>>({})
  let loaded = false
  let loading = false

  async function loadDicts() {
    if (loaded || loading) return
    loading = true
    try {
      const data = await dictApi.data(types.join(','))
      Object.entries(data).forEach(([key, items]) => {
        map[key] = items
      })
      loaded = true
    } catch {
      // 静默降级：标签显示原始值
    } finally {
      loading = false
    }
  }

  function options(type: string): DictItem[] {
    return map[type] ?? []
  }

  function label(type: string, value?: string | number | null): string {
    if (value === null || value === undefined || value === '') return '—'
    const target = String(value)
    const hit = options(type).find((item) => item.dictValue === target)
    return hit ? hit.dictLabel : target
  }

  return { map, loadDicts, options, label }
}

/** 金额展示：分转元不需要，后端已是元；null 用破折号 */
export function money(value?: number | null): string {
  if (value === null || value === undefined) return '—'
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 万元展示：漏斗 / 汇总用 */
export function wan(value?: number | null): string {
  if (value === null || value === undefined) return '—'
  return `${(value / 10000).toLocaleString('zh-CN', { maximumFractionDigits: 1 })} 万`
}

export function dateText(value?: string | null): string {
  if (!value) return '—'
  return value.length > 10 ? value.slice(0, 10) : value
}

export function dateTimeText(value?: string | null): string {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}
