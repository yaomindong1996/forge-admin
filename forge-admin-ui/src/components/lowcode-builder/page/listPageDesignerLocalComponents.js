/**
 * Local Vue components + icons used by ListPageGridDesigner shell and property panels.
 * Options API panels must register these; script-setup auto-exposure no longer applies.
 */
import {
  AddOutline,
  BrowsersOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
  CodeSlashOutline,
  ColorPaletteOutline,
  ContractOutline,
  DesktopOutline,
  EllipsisHorizontalOutline,
  ExpandOutline,
  EyeOutline,
  FlashOutline,
  HelpCircleOutline,
  PhonePortraitOutline,
  RemoveOutline,
  ReorderThreeOutline,
  ResizeOutline,
  SearchOutline,
  SettingsOutline,
  SwapHorizontalOutline,
  TabletLandscapeOutline,
} from '@vicons/ionicons5'
import draggable from 'vuedraggable'
import { DesignerNodeOverlay } from '@/components/lowcode-builder/designer-core'
import SpecPropertyPanel from '@/components/lowcode-builder/designer-core/panel/SpecPropertyPanel.vue'
import UnifiedComponentPalette from '@/components/lowcode-builder/designer-core/panel/UnifiedComponentPalette.vue'
import WidgetDataBindingEditor from '@/components/lowcode-builder/shared/WidgetDataBindingEditor.vue'
import WidgetFieldPathPicker from '@/components/lowcode-builder/shared/WidgetFieldPathPicker.vue'
import RuntimeRulesEditor from '@/components/lowcode-builder/shared/RuntimeRulesEditor.vue'
import {
  BitableAdminIcon,
  BitableAttachmentIcon,
  BitableButtonIcon,
  BitableCalendarIcon,
  BitableDragIcon,
  BitableInfoIcon,
  BitableInvisibleIcon,
  BitableLookupIcon,
  BitableMailIcon,
  BitableMemberIcon,
  BitableNumberIcon,
  BitablePhoneIcon,
  BitableSelectIcon,
  BitableStyleIcon,
  BitableTodoIcon,
  BitableVisibleIcon,
} from './bitableIcons'
import CrudDefaultParamsEditor from './CrudDefaultParamsEditor.vue'
import CrudHookRulesEditor from './CrudHookRulesEditor.vue'
import FieldConfigDrawer from './FieldConfigDrawer.vue'
import GridBlockRenderer from './GridBlockRenderer.vue'
import PageChartPropertyPanel from './PageChartPropertyPanel.vue'
import QuickEntryPropertyPanel from './QuickEntryPropertyPanel.vue'
import StatsStripPropertyPanel from './StatsStripPropertyPanel.vue'
import WorkflowTaskListPropertyPanel from './WorkflowTaskListPropertyPanel.vue'
import NoticeBoardPropertyPanel from './NoticeBoardPropertyPanel.vue'
import WelcomeBannerPropertyPanel from './WelcomeBannerPropertyPanel.vue'
import CardPropertyPanel from './CardPropertyPanel.vue'
import PanelBoxPropertyPanel from './PanelBoxPropertyPanel.vue'
import InfoPanelPropertyPanel from './InfoPanelPropertyPanel.vue'
import SpacingBoxControl from './SpacingBoxControl.vue'

export const listPageDesignerLocalComponents = {
  AddOutline,
  BrowsersOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
  CodeSlashOutline,
  ColorPaletteOutline,
  ContractOutline,
  DesktopOutline,
  EllipsisHorizontalOutline,
  ExpandOutline,
  EyeOutline,
  FlashOutline,
  HelpCircleOutline,
  PhonePortraitOutline,
  RemoveOutline,
  ReorderThreeOutline,
  ResizeOutline,
  SearchOutline,
  SettingsOutline,
  SwapHorizontalOutline,
  TabletLandscapeOutline,
  BitableAdminIcon,
  BitableAttachmentIcon,
  BitableButtonIcon,
  BitableCalendarIcon,
  BitableDragIcon,
  BitableInfoIcon,
  BitableInvisibleIcon,
  BitableLookupIcon,
  BitableMailIcon,
  BitableMemberIcon,
  BitableNumberIcon,
  BitablePhoneIcon,
  BitableSelectIcon,
  BitableStyleIcon,
  BitableTodoIcon,
  BitableVisibleIcon,
  draggable,
  DesignerNodeOverlay,
  SpecPropertyPanel,
  UnifiedComponentPalette,
  WidgetDataBindingEditor,
  WidgetFieldPathPicker,
  RuntimeRulesEditor,
  CrudDefaultParamsEditor,
  CrudHookRulesEditor,
  FieldConfigDrawer,
  GridBlockRenderer,
  PageChartPropertyPanel,
  QuickEntryPropertyPanel,
  StatsStripPropertyPanel,
  WorkflowTaskListPropertyPanel,
  NoticeBoardPropertyPanel,
  WelcomeBannerPropertyPanel,
  CardPropertyPanel,
  PanelBoxPropertyPanel,
  InfoPanelPropertyPanel,
  SpacingBoxControl,
}
