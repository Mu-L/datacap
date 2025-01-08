<template>
  <ShadcnCard :border="false">
    <ShadcnSelect v-model="selectDatabase" @on-change="onChangeDatabase">
      <template #options>
        <ShadcnSelectOption v-for="item in databaseArray" :label="item.title" :value="item.code"/>
      </template>
    </ShadcnSelect>

    <div class="relative h-screen overflow-x-auto overflow-y-auto">
      <ShadcnSpin v-if="loading" fixed/>

      <ShadcnTree v-model="databaseModel"
                  :data="dataTreeArray"
                  :loadData="onLoadData"
                  @on-node-click="onNodeClick">
        <template #label="{ node }">
          <div class="flex items-center space-x-1" @contextmenu.prevent="visibleContextMenu($event, node)">
            <ShadcnIcon v-if="node.level === StructureEnum.TYPE && node.type === '表'"
                        class="text-xs font-semibold text-gray-500"
                        size="16"
                        icon="Table"/>
            <ShadcnIcon v-else-if="node.level === StructureEnum.TYPE && node.type === '视图'"
                        class="text-xs font-semibold text-gray-500"
                        size="16"
                        icon="View"/>
            <ShadcnIcon v-else-if="node.level === StructureEnum.TYPE && node.type === '函数'"
                        class="text-xs font-semibold text-gray-500"
                        size="16"
                        icon="SquareFunction"/>
            <ShadcnIcon v-else-if="node.level === StructureEnum.TYPE && node.type === '存储过程'"
                        class="text-xs font-semibold text-gray-500"
                        size="16"
                        icon="Cpu"/>

            <span class="text-xs font-normal text-gray-900">
              {{ node.title }}
            </span>

            <span v-if="node.level === StructureEnum.COLUMN" class="text-xs font-normal text-gray-500 ml-1">
              {{ getColumnTitle(String(node.type), String(node.extra), String(node.isKey), String(node.defaultValue)) }}
            </span>
          </div>
        </template>
      </ShadcnTree>

      <ShadcnContextMenu v-if="contextmenu.visible" v-model="contextmenu.visible" :position="contextmenu.position">
        <ShadcnContextMenuSub :label="$t('source.common.menuNew')">
          <ShadcnContextMenuItem v-if="dataInfo?.level === StructureEnum.TABLE" @click="visibleCreateTable(true)">
            <div class="flex items-center space-x-1">
              <ShadcnIcon icon="Table" size="15"/>
              <span>{{ $t('source.common.menuNewTable') }}</span>
            </div>
          </ShadcnContextMenuItem>

          <ShadcnContextMenuItem @click="visibleCreateColumn(true)">
            <div class="flex items-center space-x-1">
              <ShadcnIcon icon="Columns" size="15"/>
              <span>{{ $t('source.common.newColumn') }}</span>
            </div>
          </ShadcnContextMenuItem>
        </ShadcnContextMenuSub>

        <ShadcnContextMenuSub v-if="dataInfo?.level === StructureEnum.TABLE" :label="$t('source.common.menuExport')">
          <ShadcnContextMenuItem @click="visibleExportData(true)">
            <div class="flex items-center space-x-1">
              <ShadcnIcon icon="ArrowUpFromLine" size="15"/>
              <span>{{ $t('source.common.exportData') }}</span>
            </div>
          </ShadcnContextMenuItem>
        </ShadcnContextMenuSub>

        <ShadcnContextMenuItem v-if="dataInfo?.level === StructureEnum.TABLE" @click="visibleTruncateTable(true)">
          <div class="flex items-center space-x-1">
            <ShadcnIcon icon="Trash" size="15"/>
            <span>{{ $t('source.common.truncateTable') }}</span>
          </div>
        </ShadcnContextMenuItem>

        <ShadcnContextMenuItem v-if="dataInfo?.level === StructureEnum.TABLE" @click="visibleDropTable(true)">
          <div class="flex items-center space-x-1">
            <ShadcnIcon icon="Delete" size="15"/>
            <span>{{ $t('source.common.dropTable') }}</span>
          </div>
        </ShadcnContextMenuItem>

        <ShadcnContextMenuItem v-if="dataInfo?.level === StructureEnum.COLUMN" @click="visibleChangeColumn(true)">
          <div class="flex items-center space-x-1">
            <ShadcnIcon icon="Pencil" size="15"/>
            <span>{{ $t('source.common.changeColumn') }}</span>
          </div>
        </ShadcnContextMenuItem>

        <ShadcnContextMenuItem v-if="dataInfo?.level === StructureEnum.COLUMN" @click="visibleDropColumn(true)">
          <div class="flex items-center space-x-1">
            <ShadcnIcon icon="Delete" size="15"/>
            <span>{{ $t('source.common.dropColumn') }}</span>
          </div>
        </ShadcnContextMenuItem>
      </ShadcnContextMenu>
    </div>
  </ShadcnCard>

  <TableCreate v-if="tableCreateVisible"
               :is-visible="tableCreateVisible"
               :info="dataInfo as any"
               @close="visibleCreateTable(false)"/>

  <ColumnCreate v-if="columnCreateVisible"
                :is-visible="columnCreateVisible"
                :info="dataInfo as any"
                @close="visibleCreateColumn(false)"/>

  <TableExport v-if="tableExportVisible"
               :isVisible="tableExportVisible"
               :info="dataInfo as any"
               @close="visibleExportData(false)"/>

  <TableTruncate v-if="tableTruncateVisible"
                 :is-visible="tableTruncateVisible"
                 :info="dataInfo as any"
                 @close="visibleTruncateTable(false)"/>

  <TableDrop v-if="tableDropVisible"
             :is-visible="tableDropVisible"
             :info="dataInfo as any"
             @close="visibleDropTable(false)"/>

  <ColumnChange v-if="columnChangeVisible"
                :is-visible="columnChangeVisible"
                :info="dataInfo as any"
                @close="visibleChangeColumn(false)"/>

  <ColumnDrop v-if="columnDropVisible"
              :is-visible="columnDropVisible"
              :info="dataInfo as any"
              @close="visibleDropColumn(false)"/>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import MetadataService from '@/services/metadata.ts'
import { StructureEnum, StructureModel } from '@/model/structure.ts'
import ColumnCreate from '@/views/pages/admin/source/components/ColumnCreate.vue'
import ColumnDrop from '@/views/pages/admin/source/components/ColumnDrop.vue'
import TableExport from '@/views/pages/admin/source/components/TableExport.vue'
import ColumnChange from '@/views/pages/admin/source/components/ColumnChange.vue'
import TableTruncate from '@/views/pages/admin/source/components/TableTruncate.vue'
import TableDrop from '@/views/pages/admin/source/components/TableDrop.vue'
import TableCreate from '@/views/pages/admin/source/components/TableCreate.vue'

interface MenuItem
{
  type: string;        // 节点类型（表、视图等）
  title: string;        // 节点名称
  comment?: string;    // 注释
  isLeaf?: boolean;
  level?: StructureEnum;
  code?: string;
  children?: MenuItem[]; // 子节点
}

interface SourceData
{
  type_name: string;
  object_name: string;
  object_comment: string;
}

export default defineComponent({
  name: 'MetadataSidebar',
  computed: {
    StructureEnum()
    {
      return StructureEnum
    }
  },
  components: { TableCreate, TableDrop, TableTruncate, ColumnChange, TableExport, ColumnDrop, ColumnCreate },
  data()
  {
    return {
      loading: false,
      selectDatabase: undefined,
      databaseModel: '',
      originalSource: null as string | null,
      originalDatabase: null as string | null,
      originalTable: null as string | null,
      selectNode: null as StructureModel | null,
      databaseArray: Array<StructureModel>(),
      dataTreeArray: [] as any[],
      dataInfo: null as StructureModel | null,
      tableCreateVisible: false,
      tableExportVisible: false,
      tableTruncateVisible: false,
      tableDropVisible: false,
      columnCreateVisible: false,
      columnChangeVisible: false,
      columnDropVisible: false,
      contextmenu: {
        visible: false,
        position: { x: 0, y: 0 }
      }
    }
  },
  created()
  {
    this.originalSource = this.$route.params?.source as string
    this.handleInitialize()
  },
  methods: {
    handleInitialize()
    {
      const source = this.$route.params?.source as string
      const database = this.$route.params?.database as string
      if (source) {
        this.originalSource = source
        this.loading = true
        MetadataService.getDatabaseBySource(source)
                       .then(response => {
                         if (response.status) {
                           response.data.columns.forEach(item => {
                             const structure: StructureModel = {
                               title: item.object_name,
                               catalog: item.object_name,
                               code: item.object_name
                             }
                             this.databaseArray.push(structure)
                           })
                           if (database) {
                             this.originalDatabase = database
                             this.selectDatabase = database as any
                             this.onChangeDatabase()
                           }
                         }
                         else {
                           this.$Message.error({
                             content: response.message,
                             showIcon: true
                           })
                         }
                       })
                       .finally(() => this.loading = false)
      }
    },
    onChangeDatabase()
    {
      this.loading = true
      this.dataTreeArray = []
      MetadataService.getTablesByDatabase(this.originalSource, this.selectDatabase as string)
                     .then(response => {
                       if (response.status) {
                         this.dataTreeArray = [...this.convertToTreeData(response.data.columns, StructureEnum.TABLE)]
                       }
                       else {
                         this.$Message.error({
                           content: response.message,
                           showIcon: true
                         })
                       }
                     })
                     .finally(() => {
                       this.loading = false
                       const table = this.$route.params?.table
                       if (table) {
                         const node = this.dataTreeArray.find(item => item.code === table)
                         if (node) {
                           node.selected = true
                           this.handlerSelectNode([node])
                         }
                       }
                       else {
                         this.$router.push(`/admin/source/${ this.originalSource }/d/${ this.selectDatabase }`)
                       }
                     })
    },
    onNodeClick(node: any)
    {
      if (node.level === StructureEnum.TYPE) {
        return
      }

      const type = this.$route.meta.type
      this.$router.push(`/admin/source/${ this.originalSource }/d/${ this.selectDatabase }/t/${ type ? type : 'info' }/${ node.value }`)
    },
    onLoadData(item: StructureModel, callback: any)
    {
      const dataChildArray = [] as StructureModel[]
      if (item.level === StructureEnum.COLUMN) {
        callback(dataChildArray)
        return
      }
      ColumnService.getAllByTable(String(item.value))
                   .then(response => {
                     if (response.status) {
                       response.data.forEach((item: any) => {
                         dataChildArray.push({
                           title: item.name, database: item.table.database.name, databaseId: item.table.database.id, table: item.table.name,
                           tableId: item.table.code, catalog: item.catalog, value: item.code, level: StructureEnum.COLUMN, type: item.type, extra: item.extra,
                           dataType: item.dataType, engine: item.engine, isKey: item.isKey, defaultValue: item.defaultValue, children: [], isLeaf: false
                         })
                       })
                     }
                   })
                   .finally(() => callback(dataChildArray))
    },
    visibleCreateTable(opened: boolean)
    {
      this.tableCreateVisible = opened
    },
    visibleCreateColumn(opened: boolean)
    {
      this.columnCreateVisible = opened
    },
    visibleExportData(opened: boolean)
    {
      this.tableExportVisible = opened
    },
    visibleTruncateTable(opened: boolean)
    {
      this.tableTruncateVisible = opened
    },
    visibleDropTable(opened: boolean)
    {
      this.tableDropVisible = opened
    },
    visibleChangeColumn(opened: boolean)
    {
      this.columnChangeVisible = opened
    },
    visibleDropColumn(opened: boolean)
    {
      this.columnDropVisible = opened
    },
    visibleContextMenu(event: any, node: any)
    {
      this.contextmenu.position = {
        x: event.clientX,
        y: event.clientY
      }
      this.dataInfo = node
      this.contextmenu.visible = true
    },
    getColumnTitle(dataType: string, extra: string, isKey: string, defaultValue: string)
    {
      let title = dataType
      if (isKey === 'PRI') {
        if (extra) {
          title = `${ title }\u00A0(${ extra.replace('_', '\u00A0') })`
        }
        else {
          title = `${ title }`
        }
      }
      if (defaultValue && defaultValue !== 'null') {
        title = `${ title }\u00A0=\u00A0${ defaultValue }`
      }
      return title
    },
    convertToTreeData(flatData: SourceData[], level: StructureEnum = StructureEnum.DATABASE): MenuItem[]
    {
      // 按type_name分组
      const groupedData = flatData.reduce((acc, curr) => {
        if (!acc[curr.type_name]) {
          acc[curr.type_name] = []
        }
        acc[curr.type_name].push(curr)
        return acc
      }, {} as Record<string, SourceData[]>)

      // 转换为树形结构
      return Object.entries(groupedData).map(([type, items]) => ({
        type,
        title: `${ type } (${ items.length })`,
        level: StructureEnum.TYPE,
        children: items.map(item => ({
          type: 'item',
          title: item.object_name,
          level: level,
          isLeaf: false,
          code: item.object_name,
          comment: item.object_comment
        }))
      }))
    }
  }
})
</script>
