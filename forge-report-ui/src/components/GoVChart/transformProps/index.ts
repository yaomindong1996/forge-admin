import { ChatCategoryEnum, IOption } from "@/packages/components/VChart/index.d";
import bars from './bars'
import pies from './pies'
import lines from './lines'
import areas from './areas'
import funnels from "./funnels";
import wordClouds from "./wordClouds";
import scatters from "./scatters";
export const transformHandler: {
  [key: string]: (args: IOption) => any
} = {
  [ChatCategoryEnum.BAR]: bars,
  [ChatCategoryEnum.PIE]: pies,
  [ChatCategoryEnum.LINE]: lines,
  [ChatCategoryEnum.AREA]: areas,
  [ChatCategoryEnum.FUNNEL]: funnels,
  [ChatCategoryEnum.WORDCLOUD]: wordClouds,
  [ChatCategoryEnum.SCATTER]: scatters,
  // todo: more charts handler
}