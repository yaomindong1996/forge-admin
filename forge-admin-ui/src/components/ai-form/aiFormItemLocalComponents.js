/** Components for AiFormItem Options shell.
 * DictSelect / FormQuickCreateHost 用异步组件，打断
 * localComponents → DictSelect → Host → AiForm → AiFormItem → localComponents 的 TDZ 环。
 */
import { defineAsyncComponent } from 'vue'
import { NButton, NCascader, NCheckbox, NCheckboxGroup, NColorPicker, NDatePicker, NFormItem, NIcon, NInput, NInputGroup, NInputNumber, NRadio, NRadioButton, NRadioGroup, NRate, NSelect, NSlider, NSpace, NSwitch, NTimePicker, NTooltip, NTransfer, NTreeSelect, NUpload } from 'naive-ui'
import { CopyOutline } from '@vicons/ionicons5'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import IconRenderer from '@/components/IconRenderer.vue'
import FileUpload from '@/components/file-upload/index.vue'
import ImageUpload from '@/components/image-upload/index.vue'
import FieldValueRenderer from '@/components/lowcode-builder/shared/FieldValueRenderer.vue'
import PageWidgetRenderer from '@/components/lowcode-builder/shared/PageWidgetRenderer.vue'
import RegionTreeSelect from '@/components/RegionTreeSelect.vue'
import AiCustomSelect from './AiCustomSelect.vue'
import AiFormDatePicker from './AiFormDatePicker.vue'
import AiFormArrayField from './AiFormArrayField.vue'
import AiFormGroupTitle from './AiFormGroupTitle.vue'
import AiFormSectionTitle from './AiFormSectionTitle.vue'
import AiRecordSelectorModal from './AiRecordSelectorModal.vue'
import FormQuickCreateTrigger from './quick-create/FormQuickCreateTrigger.vue'

const DictSelect = defineAsyncComponent(() => import('@/components/DictSelect.vue'))
const FormQuickCreateHost = defineAsyncComponent(() => import('./quick-create/FormQuickCreateHost.vue'))

export const aiFormItemLocalComponents = {
  NButton,
  NCascader,
  NCheckbox,
  NCheckboxGroup,
  NColorPicker,
  NDatePicker,
  NFormItem,
  NIcon,
  NInput,
  NInputGroup,
  NInputNumber,
  NRadio,
  NRadioButton,
  NRadioGroup,
  NRate,
  NSelect,
  NSlider,
  NSpace,
  NSwitch,
  NTimePicker,
  NTooltip,
  NTransfer,
  NTreeSelect,
  NUpload,
  UserSelectPicker,
  IconRenderer,
  DictSelect,
  FileUpload,
  ImageUpload,
  FieldValueRenderer,
  PageWidgetRenderer,
  RegionTreeSelect,
  AiCustomSelect,
  AiFormDatePicker,
  AiFormArrayField,
  AiFormGroupTitle,
  AiFormSectionTitle,
  AiRecordSelectorModal,
  FormQuickCreateHost,
  FormQuickCreateTrigger,
  CopyOutline,
}
