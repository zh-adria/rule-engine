/**
 * Simple line-based diff utility for comparing DRL content versions
 */

/**
 * Compare two texts and return diff hunks
 * @param {string} oldText - Original text
 * @param {string} newText - Modified text
 * @returns {Array} Array of diff hunks with type: 'equal', 'add', 'remove'
 */
export function computeDiff(oldText, newText) {
  if (!oldText && !newText) return []
  if (!oldText) return [{ type: 'add', lines: newText.split('\n'), oldStart: 0, newStart: 0 }]
  if (!newText) return [{ type: 'remove', lines: oldText.split('\n'), oldStart: 0, newStart: 0 }]

  const oldLines = oldText.split('\n')
  const newLines = newText.split('\n')

  // LCS-based diff
  const lcs = buildLCS(oldLines, newLines)
  const hunks = []
  let oldIdx = 0, newIdx = 0, lcsIdx = 0

  while (oldIdx < oldLines.length || newIdx < newLines.length) {
    if (lcsIdx < lcs.length && oldIdx < oldLines.length && newIdx < newLines.length
        && oldLines[oldIdx] === lcs[lcsIdx] && newLines[newIdx] === lcs[lcsIdx]) {
      // Equal line
      if (hunks.length > 0 && hunks[hunks.length - 1].type === 'equal') {
        hunks[hunks.length - 1].lines.push(oldLines[oldIdx])
      } else {
        hunks.push({ type: 'equal', lines: [oldLines[oldIdx]], oldStart: oldIdx, newStart: newIdx })
      }
      oldIdx++
      newIdx++
      lcsIdx++
    } else if (lcsIdx < lcs.length && newIdx < newLines.length && newLines[newIdx] === lcs[lcsIdx]) {
      // Removed from old
      if (hunks.length > 0 && hunks[hunks.length - 1].type === 'remove') {
        hunks[hunks.length - 1].lines.push(oldLines[oldIdx])
      } else {
        hunks.push({ type: 'remove', lines: [oldLines[oldIdx]], oldStart: oldIdx, newStart: newIdx })
      }
      oldIdx++
    } else if (lcsIdx < lcs.length && oldIdx < oldLines.length && oldLines[oldIdx] === lcs[lcsIdx]) {
      // Added in new
      if (hunks.length > 0 && hunks[hunks.length - 1].type === 'add') {
        hunks[hunks.length - 1].lines.push(newLines[newIdx])
      } else {
        hunks.push({ type: 'add', lines: [newLines[newIdx]], oldStart: oldIdx, newStart: newIdx })
      }
      newIdx++
    } else {
      // Both differ - remove old, add new
      if (oldIdx < oldLines.length) {
        if (hunks.length > 0 && hunks[hunks.length - 1].type === 'remove') {
          hunks[hunks.length - 1].lines.push(oldLines[oldIdx])
        } else {
          hunks.push({ type: 'remove', lines: [oldLines[oldIdx]], oldStart: oldIdx, newStart: newIdx })
        }
        oldIdx++
      }
      if (newIdx < newLines.length) {
        if (hunks.length > 0 && hunks[hunks.length - 1].type === 'add') {
          hunks[hunks.length - 1].lines.push(newLines[newIdx])
        } else {
          hunks.push({ type: 'add', lines: [newLines[newIdx]], oldStart: oldIdx, newStart: newIdx })
        }
        newIdx++
      }
    }
  }

  return hunks
}

/**
 * Build Longest Common Subsequence
 */
function buildLCS(a, b) {
  const m = a.length, n = b.length
  const dp = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0))

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      dp[i][j] = a[i - 1] === b[j - 1] ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1])
    }
  }

  // Backtrack to find the LCS
  const result = []
  let i = m, j = n
  while (i > 0 && j > 0) {
    if (a[i - 1] === b[j - 1]) {
      result.unshift(a[i - 1])
      i--
      j--
    } else if (dp[i - 1][j] > dp[i][j - 1]) {
      i--
    } else {
      j--
    }
  }
  return result
}

/**
 * Count diff statistics
 */
export function diffStats(hunks) {
  let added = 0, removed = 0, unchanged = 0
  for (const hunk of hunks) {
    switch (hunk.type) {
      case 'add': added += hunk.lines.length; break
      case 'remove': removed += hunk.lines.length; break
      case 'equal': unchanged += hunk.lines.length; break
    }
  }
  return { added, removed, unchanged, total: added + removed + unchanged }
}
