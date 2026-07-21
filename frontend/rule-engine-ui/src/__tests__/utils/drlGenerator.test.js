import { describe, it, expect } from 'vitest'
import { INSURANCE_FIELDS, OPERATORS } from '../../utils/drlGenerator'

describe('drlGenerator - Field and Operator Definitions', () => {
  it('INSURANCE_FIELDS should have desc property for all fields', () => {
    for (const field of INSURANCE_FIELDS) {
      expect(field.desc).toBeDefined()
      expect(field.desc.length).toBeGreaterThan(0)
    }
  })

  it('INSURANCE_FIELDS should have required properties', () => {
    for (const field of INSURANCE_FIELDS) {
      expect(field.label).toBeDefined()
      expect(field.value).toBeDefined()
      expect(field.type).toBeDefined()
      expect(['string', 'number', 'boolean']).toContain(field.type)
    }
  })

  it('OPERATORS should have desc property for all operators', () => {
    for (const op of OPERATORS) {
      expect(op.desc).toBeDefined()
      expect(op.desc.length).toBeGreaterThan(0)
    }
  })

  it('OPERATORS should filter correctly by field type', () => {
    const numberOps = OPERATORS.filter(op => op.types.includes('number'))
    const stringOps = OPERATORS.filter(op => op.types.includes('string'))

    expect(numberOps.find(o => o.value === '>')).toBeDefined()
    expect(numberOps.find(o => o.value === 'between')).toBeDefined()
    expect(stringOps.find(o => o.value === 'contains')).toBeDefined()
    expect(stringOps.find(o => o.value === '>')).toBeUndefined()
  })

  it('should have specific field descriptions', () => {
    const bmi = INSURANCE_FIELDS.find(f => f.value === 'bmi')
    expect(bmi?.desc).toContain('体重')

    const age = INSURANCE_FIELDS.find(f => f.value === 'age')
    expect(age?.desc).toContain('周岁')

    const smoking = INSURANCE_FIELDS.find(f => f.value === 'smokingStatus')
    expect(smoking?.desc).toContain('NEVER')
  })

  it('should have specific operator descriptions', () => {
    const between = OPERATORS.find(o => o.value === 'between')
    expect(between?.desc).toContain('边界')

    const isNull = OPERATORS.find(o => o.value === 'isNull')
    expect(isNull?.desc).toContain('空')

    const matches = OPERATORS.find(o => o.value === 'matches')
    expect(matches?.desc).toContain('正则')
  })

  it('between operator should include boundary', () => {
    const between = OPERATORS.find(o => o.value === 'between')
    expect(between?.desc).toContain('含边界')
  })

  it('all fields should have unique values', () => {
    const values = INSURANCE_FIELDS.map(f => f.value)
    const uniqueValues = [...new Set(values)]
    expect(values.length).toBe(uniqueValues.length)
  })

  it('all operators should have unique values', () => {
    const values = OPERATORS.map(o => o.value)
    const uniqueValues = [...new Set(values)]
    expect(values.length).toBe(uniqueValues.length)
  })
})
