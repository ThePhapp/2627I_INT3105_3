import type { FeatureRoute } from '../../app/feature-routes'
import { DisasterPage } from './DisasterPage'

export const routes: FeatureRoute[] = [{
  path: '/operations/disasters',
  label: 'Thảm họa',
  roles: ['AUTHORITY'],
  element: <DisasterPage />,
}]
