import { useState, type FormEvent } from 'react'
import { Field } from '../../shared/components'
import {
  disasterTypes,
  severities,
  type Disaster,
  type DisasterFields,
  type DisasterPatch,
  type DisasterType,
  type Severity,
} from './disaster-api'

type FormValue = Omit<DisasterFields, 'latitude' | 'longitude'> & { latitude: string; longitude: string }
type Errors = Partial<Record<keyof FormValue | 'form', string>>

const typeLabels: Record<DisasterType, string> = {
  EARTHQUAKE: 'Động đất', FLOOD: 'Lũ lụt', TYPHOON: 'Bão', WILDFIRE: 'Cháy rừng', TSUNAMI: 'Sóng thần',
}
const severityLabels: Record<Severity, string> = {
  LOW: 'Thấp', MODERATE: 'Trung bình', HIGH: 'Cao', CRITICAL: 'Nghiêm trọng',
}

function valueOf(disaster?: Disaster): FormValue {
  return disaster ? {
    name: disaster.name,
    type: disaster.type,
    severity: disaster.severity,
    description: disaster.description,
    latitude: String(disaster.latitude),
    longitude: String(disaster.longitude),
  } : { name: '', type: 'FLOOD', severity: 'MODERATE', description: '', latitude: '', longitude: '' }
}

function validate(value: FormValue): { errors: Errors; fields?: DisasterFields } {
  const errors: Errors = {}
  const name = value.name.trim()
  const description = value.description.trim()
  const latitude = Number(value.latitude)
  const longitude = Number(value.longitude)
  if (!name || [...name].length > 120) errors.name = 'Nhập tên từ 1 đến 120 ký tự.'
  if (!description || [...description].length > 2000) errors.description = 'Nhập mô tả từ 1 đến 2.000 ký tự.'
  if (value.latitude.trim() === '' || !Number.isFinite(latitude) || latitude < -90 || latitude > 90) errors.latitude = 'Vĩ độ phải nằm trong khoảng -90 đến 90.'
  if (value.longitude.trim() === '' || !Number.isFinite(longitude) || longitude < -180 || longitude > 180) errors.longitude = 'Kinh độ phải nằm trong khoảng -180 đến 180.'
  return Object.keys(errors).length ? { errors } : {
    errors,
    fields: { name, description, latitude, longitude, type: value.type, severity: value.severity },
  }
}

export function DisasterForm({ disaster, busy, onCancel, onCreate, onEdit }: {
  disaster?: Disaster
  busy: boolean
  onCancel: () => void
  onCreate: (fields: DisasterFields) => Promise<void>
  onEdit: (patch: DisasterPatch) => Promise<void>
}) {
  const [value, setValue] = useState<FormValue>(() => valueOf(disaster))
  const [errors, setErrors] = useState<Errors>({})
  const editing = Boolean(disaster)

  function set<K extends keyof FormValue>(field: K, next: FormValue[K]) {
    setValue(current => ({ ...current, [field]: next }))
    setErrors(current => ({ ...current, [field]: undefined, form: undefined }))
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    const checked = validate(value)
    if (!checked.fields) { setErrors(checked.errors); return }
    if (!disaster) { await onCreate(checked.fields); return }
    const patch: DisasterPatch = { expectedVersion: disaster.version }
    for (const key of ['name', 'type', 'severity', 'description', 'latitude', 'longitude'] as const) {
      if (checked.fields[key] !== disaster[key]) patch[key] = checked.fields[key] as never
    }
    if (Object.keys(patch).length === 1) {
      setErrors({ form: 'Chưa có thay đổi để lưu.' })
      return
    }
    await onEdit(patch)
  }

  return <form className="disaster-form" onSubmit={submit} noValidate aria-busy={busy}>
    <Field id="disaster-name" label="Tên thảm họa" value={value.name} maxLength={120}
      onChange={event => set('name', event.target.value)} error={errors.name} disabled={busy} required />
    <div className="disaster-form__pair">
      <div className="field"><label htmlFor="disaster-type">Loại thảm họa</label>
        <select id="disaster-type" value={value.type} onChange={event => set('type', event.target.value as DisasterType)} disabled={busy}>
          {disasterTypes.map(type => <option key={type} value={type}>{typeLabels[type]}</option>)}
        </select>
      </div>
      <div className="field"><label htmlFor="disaster-severity">Mức độ</label>
        <select id="disaster-severity" value={value.severity} onChange={event => set('severity', event.target.value as Severity)} disabled={busy}>
          {severities.map(severity => <option key={severity} value={severity}>{severityLabels[severity]}</option>)}
        </select>
      </div>
    </div>
    <div className="field"><label htmlFor="disaster-description">Mô tả</label>
      <textarea id="disaster-description" value={value.description} maxLength={2000} rows={5}
        onChange={event => set('description', event.target.value)} aria-invalid={Boolean(errors.description)}
        aria-describedby={errors.description ? 'disaster-description-error' : undefined} disabled={busy} required />
      {errors.description && <span id="disaster-description-error" className="field-error">{errors.description}</span>}
    </div>
    <div className="disaster-form__pair">
      <Field id="disaster-latitude" label="Vĩ độ" type="number" step="any" value={value.latitude}
        onChange={event => set('latitude', event.target.value)} error={errors.latitude} disabled={busy} required />
      <Field id="disaster-longitude" label="Kinh độ" type="number" step="any" value={value.longitude}
        onChange={event => set('longitude', event.target.value)} error={errors.longitude} disabled={busy} required />
    </div>
    {errors.form && <p className="field-error" role="alert">{errors.form}</p>}
    <div className="disaster-actions">
      <button type="submit" disabled={busy}>{busy ? 'Đang lưu…' : editing ? 'Lưu thay đổi' : 'Tạo thảm họa'}</button>
      <button type="button" className="secondary" onClick={onCancel} disabled={busy}>Hủy</button>
    </div>
  </form>
}

export { severityLabels, typeLabels }
