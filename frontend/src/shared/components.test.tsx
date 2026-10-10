import { expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Field } from './components'

it('associates external instructions, hint and validation error without losing any description', () => {
  const { rerender } = render(<><p id="instructions">WGS84.</p><Field id="latitude" label="Vĩ độ" hint="Từ -90 đến 90." error="Giá trị ngoài miền." aria-describedby="instructions" /></>)
  expect(screen.getByLabelText('Vĩ độ')).toHaveAccessibleDescription('WGS84. Từ -90 đến 90. Giá trị ngoài miền.')
  expect(screen.getByLabelText('Vĩ độ')).toHaveAttribute('aria-invalid', 'true')
  rerender(<><p id="instructions">WGS84.</p><Field id="latitude" label="Vĩ độ" hint="Từ -90 đến 90." aria-describedby="instructions" /></>)
  expect(screen.getByLabelText('Vĩ độ')).toHaveAccessibleDescription('WGS84. Từ -90 đến 90.')
  expect(screen.getByLabelText('Vĩ độ')).toHaveAttribute('aria-invalid', 'false')
})
