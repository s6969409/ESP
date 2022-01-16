using Newtonsoft.Json.Linq;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Data;
using System.IO;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;

namespace BomHelper
{
    class Bom
    {
        public static readonly string folderPathDatas = Directory.GetCurrentDirectory() + "\\BomItemDatas";
        private static readonly string DB = folderPathDatas + "\\DB.xlsx";

        private static DateTime libLastWriteTime;
        private static ItemBom libAtDisk;

        #region data struct...ItemBase,ItemBom,
        public enum Source { P, U, M };
        public static readonly string[] parentNames = { "電控總組立組", "電氣箱組立組", "配電盤組立組" };
        public static readonly ItemBom[] parentsDefine = new ItemBom[]
        {
            new ItemBom(){ name = parentNames[0]},
            new ItemBom(){ name = parentNames[1]},
            new ItemBom(){ name = parentNames[2]}
        };
        public static readonly SolidColorBrush[] parentNamesBrush = { Brushes.DeepSkyBlue, Brushes.Violet, Brushes.YellowGreen };

        public static int getIndex(string[] s, string o)
        {
            for (int i = 0; i < s.Length; i++)
            {
                if (s[i].Equals(o)) return i;
            }
            return -1;
        }
        
        public class ItemBom : IUIProperty, INotifyPropertyChanged
        {
            protected string _catalog = "";
            public virtual string catalog
            {
                get { return _catalog; }
                set
                {
                    ItemBom search = Bom.search(value);
                    if (search != null)
                    {
                        name = search.name;
                        specification = search.specification;
                        //source = search.source;
                        //content = search.content;
                    }
                    _catalog = value;
                    onPropertyChanged(nameof(catalog));
                }
            }
            private string _name = "";
            public string name
            {
                get { return _name; }
                set
                {
                    _name = value;
                    foreach (string n in parentNames) 
                        if (value.Equals(n)) 
                            source = Source.M;
                    onPropertyChanged(nameof(name));
                }
            }
            private string _specification = "";
            public string specification
            {
                get { return _specification; }
                set
                {
                    _specification = value;
                    onPropertyChanged(nameof(specification));
                }
            }
            private Source _source = Source.P;
            public Source source
            {
                get { return _source; }
                set
                {
                    _source = value;
                    onPropertyChanged(nameof(source));
                }
            }
            public ObservableRangeCollection<ItemBom> content{ get; set; }

            public ItemBom parent;
            public string parentName
            {
                get { return parent == null ? "?" : parent.name; }
                set
                {
                    for (int i = 0; i < parentNames.Length; i++)
                    {
                        if (value.Equals(parentsDefine[i].name))
                        {
                            parent = parentsDefine[i];
                            onPropertyChanged(nameof(parentName));
                        }
                    }
                }
            }
            public string parentCatalog => parent == null ? "?" : parent.catalog;

            private int _quantity = 1;
            public int quantity
            {
                get { return _quantity; }
                set
                {
                    _quantity = value;
                    onPropertyChanged(nameof(quantity));
                }
            }
            private string _remark = "";
            public string remark
            {
                get { return _remark; }
                set
                {
                    _remark = value;
                    onPropertyChanged(nameof(remark));
                }
            }

            public event PropertyChangedEventHandler PropertyChanged;
            protected void onPropertyChanged(string propertyName)
            {
                if (PropertyChanged != null)
                {
                    PropertyChanged.Invoke(this, new PropertyChangedEventArgs(propertyName));
                }
            }

            public ItemBom() { initChild(); }
            public ItemBom(string catalog)
            {
                _catalog = catalog;
                initChild();
            }
            public ItemBom(string parentName, string catalog, string name,
                string specification, int quantity, string remark)
            {
                this._catalog = catalog;
                this.name = name;
                this.specification = specification;
                this.parentName = parentName;
                this.quantity = quantity;
                this.remark = remark;
                initChild();
            }

            public ItemBom(JObject jObject)
            {
                string pn = jObject.Value<string>(nameof(parentName));
                for (int i = 0; i < parentNames.Length; i++)
                {
                    if (pn.Equals(parentsDefine[i].name))
                    {
                        parent = parentsDefine[i];
                    }
                }
                _catalog = jObject.Value<string>(nameof(catalog));
                name = jObject.Value<string>(nameof(name));
                specification = jObject.Value<string>(nameof(specification));
                quantity = jObject.Value<int>(nameof(quantity));
                remark = jObject.Value<string>(nameof(remark));

                source = (Source)Enum.Parse(typeof(Source),
                    (jObject.Value<int>(nameof(source))).ToString());
                JArray jArray = jObject.Value<JArray>(nameof(content));
                content = buildBomObj(jArray);
            }
            private void initChild()
            {
                content = new ObservableRangeCollection<ItemBom>(
                    //onChildAdd, onChildRemove);
                    null, null);
            }

            #region UI control properties
            private bool _isExpanded = false;
            public bool isExpanded
            {
                get { return _isExpanded; }
                set
                {
                    _isExpanded = value;
                    onPropertyChanged(nameof(isExpanded));
                }
            }
            public int FontSize => WindowPreference.getCfgValue<int>(WindowPreference.fontSize);
            public Brush RowHeaderBachGroud
            {
                get
                {
                    for (int i = 0; i < parentNames.Length; i++)
                    {
                        if (parentName.Equals(parentNames[i]))
                        {
                            return parentNamesBrush[i];
                        }
                    }
                    return Brushes.MediumPurple;
                }
            }
            #endregion

            public JObject getJObject()
            {
                JArray jArray = new JArray();
                foreach (ItemBom i in content)
                {
                    jArray.Add(i.getJObject());
                }

                JObject jObject = new JObject()
                {
                    {nameof(catalog),catalog },
                    {nameof(name),name },
                    {nameof(specification),specification },
                    {nameof(source),source.GetHashCode() },
                    {nameof(content),jArray }
                };
                jObject.Add(nameof(parentName), parentName);
                jObject.Add(nameof(quantity), quantity);
                jObject.Add(nameof(remark), remark);
                return jObject;
            }
            public bool equals(ItemBom item)
            {
                bool b = true;
                b &= catalog.Equals(item.catalog);
                b &= name.Equals(item.name);
                b &= specification.Equals(item.specification);
                b &= source == item.source;

                b &= content.Count == item.content.Count;
                for(int i = 0; b && i < content.Count; i++)
                {
                    b &= equals(item.content[i]);
                }

                b &= parentName.Equals(item.parentName);
                b &= quantity.Equals(item.quantity);
                b &= remark.Equals(item.remark);
                return b;
            }
            public bool equalsIgnore(ItemBom item, Ignore ignore)
            {
                if (item == null) return false;

                bool b = true;
                b &= catalog.Equals(item.catalog);
                b &= ignore.name || name.Equals(item.name);
                b &= ignore.specification || specification.Equals(item.specification);
                b &= source == item.source;
                b &= content.Count == item.content.Count;
                for (int i = 0; b && i < content.Count; i++)
                {
                    b &= equals(item.content[i]);
                }

                b &= ignore.parent || parentName.Equals(item.parentName);
                b &= quantity.Equals(item.quantity);
                b &= remark.Equals(item.remark);
                return b;
            }
            public ItemBom depthClone()
            {
                ItemBom item = new ItemBom(this.catalog.Clone() as string);
                item.name = name.Clone() as string;
                item.specification = specification.Clone() as string;
                item.source = source;
                item.parent = parent;
                item.quantity = quantity;
                item.remark = remark.Clone() as string;
                foreach (ItemBom i in content)
                {
                    ItemBom copy = i.depthClone();
                    copy.parent = item;//X
                    item.content.Add(copy);
                }
                return item;
            }
            public ItemBom[] PItems {
                get {
                    List<ItemBom> top = new List<ItemBom>();
                    top.AddRange(content);
                    for (int i = 0; i < top.Count; i++)
                    {
                        ItemBom pt = top[i];
                        if (pt.source != Source.P || pt.content.Count > 0)
                        {
                            top.RemoveAt(i);
                            top.AddRange(pt.content);
                            i--;
                        }
                    }
                    return top.ToArray();
                }
            }
            public string summary
            {
                get
                {
                    string info = "";
                    info += nameof(catalog) + ": " + catalog + "\n";
                    info += nameof(name) + ": " + name + "\n";
                    info += nameof(specification) + ": " + specification + "\n";
                    return info;
                }
            }

            private bool isParentDefine
            {
                get
                {
                    foreach(ItemBom parentDefine in parentsDefine)
                    {
                        if (parent == parentDefine) return true;
                    }
                    return false;
                }
            }
            protected void onChildAdd(ItemBom node)
            {
                if (!node.isParentDefine)
                {
                    node.parent = this;
                }
            }
            protected static void onChildRemove(ItemBom node)
            {
                node.parent = null;
            }
        }
        public static class Properties
        {
            private static string catalog { get; } = nameof(ItemBom.catalog);
            private static string name { get; } = nameof(ItemBom.name);
            private static string specification { get; } = nameof(ItemBom.specification);
            private static string source { get; } = nameof(ItemBom.source);
            private static string content { get; } = nameof(ItemBom.content);
            private static string parentCatalog { get; } = nameof(ItemBom.parentCatalog);
            private static string parentName { get; } = nameof(ItemBom.parentName);
            private static string quantity { get; } = nameof(ItemBom.quantity);
            private static string remark { get; } = nameof(ItemBom.remark);

            public static string[] basePs { get; } = new string[] { catalog, name, specification };
            public static string[] bomShowPs { get; } = new string[] { catalog, name, specification, quantity, remark };
            public static string[] bomShowPs2 { get; } = new string[] { parentName, catalog, name, specification, quantity, remark };

            public static string[] bomSavePs { get; } = new string[] { parentCatalog, parentName, catalog, name, specification, quantity, remark };

        }
        private static class ColumnName
        {
            public static string catalog(XlsxFormat format)
            {
                switch (format)
                {
                    case XlsxFormat.YjIt:
                        return "cp_item_no";
                    case XlsxFormat.DukeDB:
                    case XlsxFormat.DukeBom:
                    case XlsxFormat.DukeBom2:
                        return nameof(ItemBom.catalog);
                    default:
                        throw new Exception("XlsxFormat not found");
                }
            }
            public static string name(XlsxFormat format)
            {
                switch (format)
                {
                    case XlsxFormat.YjIt:
                        return "cp_ITEM_NM";
                    case XlsxFormat.DukeDB:
                    case XlsxFormat.DukeBom:
                    case XlsxFormat.DukeBom2:
                        return nameof(ItemBom.name);
                    default:
                        throw new Exception("XlsxFormat not found");
                }
            }
            public static string specification(XlsxFormat format)
            {
                switch (format)
                {
                    case XlsxFormat.YjIt:
                        return "cp_ITEM_SP";
                    case XlsxFormat.DukeDB:
                    case XlsxFormat.DukeBom:
                    case XlsxFormat.DukeBom2:
                        return nameof(ItemBom.specification);
                    default:
                        throw new Exception("XlsxFormat not found");
                }
            }
            public static string source(XlsxFormat format)
            {
                switch (format)
                {
                    case XlsxFormat.YjIt:
                        return "c_source";
                    case XlsxFormat.DukeBom:
                    case XlsxFormat.DukeBom2:
                        return nameof(ItemBom.source);
                    default:
                        throw new Exception("XlsxFormat not found");
                }
            }
            public static string quantity(XlsxFormat format)
            {
                switch (format)
                {
                    case XlsxFormat.YjIt:
                        return "QTY_Require";
                    case XlsxFormat.DukeBom:
                    case XlsxFormat.DukeBom2:
                        return nameof(ItemBom.quantity);
                    default:
                        throw new Exception("XlsxFormat not found");
                }
            }
            public static string remark(XlsxFormat format)
            {
                switch (format)
                {
                    case XlsxFormat.YjIt:
                        return "REMK";
                    case XlsxFormat.DukeBom:
                    case XlsxFormat.DukeBom2:
                        return nameof(ItemBom.remark);
                    default:
                        throw new Exception("XlsxFormat not found");
                }
            }

            public static string parentCatalog(XlsxFormat format)
            {
                switch (format)
                {
                    case XlsxFormat.YjIt:
                        return "ITEM_NO";
                    case XlsxFormat.DukeBom:
                    case XlsxFormat.DukeBom2:
                        return nameof(ItemBom.parentCatalog);
                    default:
                        throw new Exception("XlsxFormat not found");
                }
            }
            public static string parentName(XlsxFormat format)
            {
                switch (format)
                {
                    case XlsxFormat.YjIt:
                        return "ITEM_NM_SP";
                    case XlsxFormat.DukeBom:
                    case XlsxFormat.DukeBom2:
                        return nameof(ItemBom.parentName);
                    default:
                        throw new Exception("XlsxFormat not found");
                }
            }

        }
        
        public static JArray buildJArray
            (ObservableRangeCollection<ItemBom> list)
        {
            JArray jArray = new JArray();
            if (list == null) return null;
            foreach (ItemBom t in list)
            {
                jArray.Add(t.getJObject());
            }
            return jArray;
        }
        public static ObservableRangeCollection<ItemBom> buildBomObj
            (JArray jArray)
        {
            ObservableRangeCollection<ItemBom> list 
                = new ObservableRangeCollection<ItemBom>();
            foreach (JObject jObject in jArray)
            {
                ItemBom item = new ItemBom(jObject);
                list.Add(item);
            }
            return list;
        }
        
        public class Ignore
        {
            public bool parent { get; set; }
            public bool name { get; set; }
            public bool specification { get; set; }
        }
        public static List<ItemBom> compareBom(ItemBom bom1, ItemBom bom2, Ignore ignore)
        {
            List<ItemBom> list1 = new List<ItemBom>(bom1.PItems);
            List<ItemBom> list2 = new List<ItemBom>(bom2.PItems);
            #region compare 2 lists
            for (int i = 0; i < list1.Count; i++)
            {
                for (int j = 0; j < list2.Count; j++)
                {

                    if (list1[i].equalsIgnore(list2[j], ignore))
                    {
                        list1.Remove(list1[i]);
                        list2.Remove(list2[j]);
                        i--;
                        break;
                    }
                }
            }
            #endregion

            #region quantity of list2 turn to nagative
            foreach (ItemBom item in list2)
            {
                item.quantity *= -1;
            }
            #endregion

            List<ItemBom> result = new List<ItemBom>();
            result.AddRange(list1);
            result.AddRange(list2);

            return result;
        }
        #endregion

        private static bool needReadLib
        {
            get
            {
                if (libLastWriteTime == null ||
                !libLastWriteTime.Equals(File.GetLastWriteTime(DB).ToLocalTime()))
                {
                    libLastWriteTime = File.GetLastWriteTime(DB).ToLocalTime();
                    return true;
                }
                return false;
            }
        }
        public static ItemBom readLib()
        {
            if (needReadLib) libAtDisk = readXlsxFile(DB);
            return libAtDisk.depthClone();
        }
        public static void updateToLib(ItemBom pt)
        {
            ItemBom data = readLib();

            foreach(ItemBom i in pt.content)
            {
                ItemBom remove = hadSameCatlog(data.content, i);

                if (remove != null)
                {
                    bool removeState = data.content.Remove(remove);
                    if (!removeState) MessageBox.Show("Can't remove it",
                        "updateError", MessageBoxButton.OK,
                        MessageBoxImage.Error);
                }
                data.content.Add(i);
            }

            writeToDB(DB, data.PItems, Properties.basePs);
        }
        public static void removeLibItems(ItemBom pt)
        {
            ItemBom lib = readLib();
            if (pt == null || lib == null) return;

            #region remove lib temp
            string tittle = "Information";
            string msg = string.Empty;
            MessageBoxImage icon = MessageBoxImage.Information;
            foreach (ItemBom i in pt.content)
            {
                ItemBom removeItem = hadSameCatlog(lib.content, i);
                if (removeItem == null)
                {
                    msg += "[DB don't exist]\n" + i.summary;
                    tittle = "Warning";
                    icon = MessageBoxImage.Warning;
                }
                else if (!lib.content.Remove(removeItem))
                {
                    msg += "[DB can't remove]\n" + i.summary;
                    tittle = "Warning";
                    icon = MessageBoxImage.Warning;
                }
            }
            #endregion
            if (icon == MessageBoxImage.Information)
            {
                writeToDB(DB, lib.content.ToArray(), Properties.basePs);
            }
            else
            {
                MessageBox.Show(msg, tittle, MessageBoxButton.OK, icon);
                File.WriteAllText(folderPathDatas + "\\DBErrorLog.txt", msg);
            }
        }
        public static ItemBom diffAndShortageWithDB(ItemBom newItem)
        {
            ItemBom basic = readLib();
            ItemBom item = new ItemBom();
            foreach (ItemBom i in newItem.PItems)
            {
                bool needAdd = true;
                foreach(ItemBom j in basic.PItems)
                {
                    if (i.catalog.Equals(j.catalog))
                    {
                        needAdd = !(i.name.Equals(j.name) 
                            && i.specification.Equals(j.specification));
                        break;
                    }
                }
                if(needAdd) item.content.Add(i);
            }

            return item;
        }

        public static ItemBom search(ItemBom search)
        {
            return Bom.search(null, search);
        }
        public static ItemBom search(ItemBom libNode, ItemBom search)
        {
            ItemBom bases = libNode == null ? readLib() : libNode.depthClone();
            ItemBom searchResult = new ItemBom("search");

            #region define search mode
            bool b1 = search.catalog != string.Empty
                && search.name == string.Empty
                && search.specification == string.Empty;
            bool b2 = search.catalog == string.Empty
                && search.name != string.Empty
                && search.specification != string.Empty;
            bool b3 = search.catalog == string.Empty
                && search.name != string.Empty 
                && search.specification == string.Empty;
            bool b4 = search.catalog == string.Empty
                && search.name == string.Empty
                && search.specification != string.Empty;
            #endregion

            foreach (ItemBom item in bases.PItems)
            {
                if (
                    (b1 && item.catalog.Contains(search.catalog))
                    || (b2 && item.name.Contains(search.name) && item.specification.Contains(search.specification))
                    || (b3 && item.name.Contains(search.name))
                    || (b4 && item.specification.Contains(search.specification))
                    )
                {
                    searchResult.content.Add(item);
                    item.parent = searchResult;//X
                }
            }

            return searchResult;
        }
        public static ItemBom search(string searchCatalog)
        {
            ItemBom basic = readLib();
            foreach (ItemBom item in basic.content)
            {
                item.parent = null;
                if (item.catalog.Equals(searchCatalog))
                    return item;
            }
            return null;
        }
        private static ItemBom hadSameCatlog(IEnumerable<ItemBom> lib, ItemBom cmp)
        {
            foreach (ItemBom item in lib)
            {
                if (item.catalog.Equals(cmp.catalog))
                {
                    return item;
                }
            }
            return null;
        }
        
        /**************************CSV
         * cp_item_no   catlog  [22]
         * cp_ITEM_NM   name    [23]
         * cp_ITEM_SP   specification   [24]
         * REMK 備註  [14]
         * QTY_Require  數量  [11]
         * unit 數量單位    [26]
         * VD_NM    供應商 [21]
         * ITEM_NO  主件料號    [5]
         * ITEM_NM_SP   主件名稱    [6]
         * CO_NO    工令  [3] =VCH_NO [8] 
         * c_source M=自製件,U=非存貨(組件?),P=採購件  [25]
         * 
         * 當c_source == U，必有下列條件
         * 有子成員，子成員的ITEM_NO = 父成員的cp_item_no
         * 
         * ITEM_NO = 有3種 主件編號
         * 電控總組立:22+華蓉變數
         * 電氣箱組立:22+華蓉變數+J~
         * 配電盤組立:22+華蓉變數+I~
         * 幹 覽趴!
         * 
         * ITEM_NM_SP = 有主要3種  主件名稱
         * 電控總組立組/~
         * 電氣箱組立組/~
         * 配電盤組立組/~
         * 幹 覽趴!
        */

        //private static List<string> COLUMNS = new List<string> { "CS_NO", "CS_SHORT_NM", "CO_TY", "CO_NO", "CO_SR", "ITEM_NO", "ITEM_NM_SP", "VCH_TY", "VCH_NO", "VCH_SR", "VCH_DSR", "QTY_Require", "C_PreDispatch", "VD_NO", "REMK", "ID", "ParentID", "SortId", "PreDispatch_DT", "PRCS_NO", "PST", "VD_NM", "cp_item_no", "cp_ITEM_NM", "cp_ITEM_SP", "c_source", "unit", "PRCV_DT" };
        private enum XlsxFormat { Unknow, YjIt, DukeDB, DukeBom, DukeBom2 }
        private static XlsxFormat getXlsxFormat(DataTable dt)
        {
            if (isYjIt(dt)) return XlsxFormat.YjIt;
            else if (isDukeDB(dt)) return XlsxFormat.DukeDB;
            else if (isDukeBom(dt)) return XlsxFormat.DukeBom;
            else if (isDukeBom2(dt)) return XlsxFormat.DukeBom2;
            else return XlsxFormat.Unknow;
        }
        private static bool isYjIt(DataTable dt)
        {
            return
                dt.Columns["cp_item_no"] != null
                && dt.Columns["cp_ITEM_NM"] != null
                && dt.Columns["cp_ITEM_SP"] != null
                && dt.Columns["REMK"] != null
                && dt.Columns["QTY_Require"] != null
                && dt.Columns["unit"] != null
                && dt.Columns["ITEM_NO"] != null
                && dt.Columns["ITEM_NM_SP"] != null
                && dt.Columns["c_source"] != null;
        }
        private static bool isDukeDB(DataTable dt)
        {
            return
                dt.Columns["parentCatalog"] == null
                && dt.Columns["parentName"] == null
                && dt.Columns["catalog"] != null
                && dt.Columns["name"] != null
                && dt.Columns["specification"] != null
                && dt.Columns["quantity"] == null
                && dt.Columns["remark"] == null;
        }
        private static bool isDukeBom(DataTable dt)
        {
            return
                dt.Columns["parentCatalog"] == null
                && dt.Columns["parentName"] != null
                && dt.Columns["catalog"] != null
                && dt.Columns["name"] != null
                && dt.Columns["specification"] != null
                && dt.Columns["quantity"] != null
                && dt.Columns["remark"] != null;
        }
        private static bool isDukeBom2(DataTable dt)
        {
            return
                dt.Columns["parentCatalog"] != null
                && dt.Columns["parentName"] != null
                && dt.Columns["catalog"] != null
                && dt.Columns["name"] != null
                && dt.Columns["specification"] != null
                && dt.Columns["quantity"] != null
                && dt.Columns["remark"] != null;
        }
        public static ItemBom readXlsxFile(string path)
        {
            DataTable dt = readXlsxToDT(path);
            if (dt == null) return null;

            #region check file content type, then decode it!
            XlsxFormat format = getXlsxFormat(dt);
            if (format == XlsxFormat.YjIt || format == XlsxFormat.DukeBom2)
                return readDBXlsx(dt, format);
            else if(format == XlsxFormat.DukeBom)
                return readDBXlsxByDuke1(dt, format);
            else if (format == XlsxFormat.DukeDB)
                return readDB(dt);
            else
            {
                string msg = Application.Current.FindResource("MsgDrogFileError") as string;
                MessageBox.Show(msg, "Warning", MessageBoxButton.OK, MessageBoxImage.Warning);
                return null;
            }
            #endregion
        }
        private static DataTable readXlsxToDT(string path)
        {
            try
            {
                ExcelManager manager = new ExcelManager(path);
                return manager.ExcelToDataTable(null, true);//sheetName = null,read no.1 sheet
            }
            catch (FileNotFoundException)
            {
                MessageBox.Show("File not found!", "message", MessageBoxButton.OK, MessageBoxImage.Error);
                return null;
            }
            catch (Exception ex)
            {
                string exceptionName = ex.ToString();
                int index = exceptionName.IndexOf(":");
                exceptionName = exceptionName.Substring(0, index);
                string msg = "File data format error!\n";
                msg += "Exception: " + exceptionName + "\n";
                msg += "Message: " + ex.Message;
                MessageBox.Show(msg, "message", MessageBoxButton.OK, MessageBoxImage.Error);
                return null;
            }
        }
        private static ItemBom readDBXlsxByDuke1(DataTable dt, XlsxFormat format)
        {
            #region find roots
            List<int> rootIndexes = new List<int>();
            for (int i = 0; i < dt.Rows.Count; i++)
            {
                bool isFindRoot = true;
                for (int j = 0; j < dt.Rows.Count; j++)
                {
                    if (i == j) continue;

                    if (dt.Rows[i][ColumnName.parentName(format)].ToString()
                            .Equals(dt.Rows[j][ColumnName.name(format)].ToString()))
                    {
                        isFindRoot = false;
                        break;
                    }
                }
                if (isFindRoot) rootIndexes.Add(i);
            }
            #endregion

            ItemBom item = new ItemBom();
            //build roots
            foreach (int rootIndex in rootIndexes)
            {
                ItemBom ib = new ItemBom(dt.Rows[rootIndex][ColumnName.parentName(format)].ToString());
                ib.name = dt.Rows[rootIndex][ColumnName.parentName(format)].ToString();
                bool newRoot = true;
                foreach (ItemBom ii in item.content)
                {
                    if (ii.catalog.Equals(ib.catalog))
                    {
                        newRoot = false;
                        break;
                    }
                }
                if (newRoot)
                {
                    item.content.Add(ib);
                    ib.parent = item;//X
                }
            }

            //add subs
            while (dt.Rows.Count != 0)
            {
                List<ItemBom> itemBoms = new List<ItemBom>(item.content);
                for (int i = 0; i < itemBoms.Count; i++)
                {
                    for (int j = 0; j < dt.Rows.Count; j++)
                    {
                        if (itemBoms[i].name
                            .Equals(dt.Rows[j][ColumnName.parentName(format)].ToString()))
                        {
                            //add Data
                            ItemBom add = new ItemBom();
                            add.catalog = dt.Rows[j][ColumnName.catalog(format)].ToString();
                            add.name = dt.Rows[j][ColumnName.name(format)].ToString();
                            add.specification = dt.Rows[j][ColumnName.specification(format)].ToString();
                            add.source = Source.P;
                            if (format == XlsxFormat.YjIt && dt.Rows[j][ColumnName.source(format)].ToString().Equals("U"))
                                add.source = Source.U;
                            if (format == XlsxFormat.YjIt && dt.Rows[j][ColumnName.source(format)].ToString().Equals("M"))
                                add.source = Source.M;
                            add.quantity = int.Parse(dt.Rows[j][ColumnName.quantity(format)].ToString());
                            add.remark = dt.Rows[j][ColumnName.remark(format)].ToString();

                            dt.Rows.RemoveAt(j);
                            j--;
                            itemBoms.Add(add);

                            //find root to add
                            rootIsFound = false;
                            findRoot(item.content, add);
                            add.parent = itemBoms[i];//X留,否則會被findRoot 蓋掉
                        }
                    }
                }
            }

            return item;
        }
        private static ItemBom readDBXlsx(DataTable dt, XlsxFormat format)
        {
            #region find roots
            List<int> rootIndexes = new List<int>();
            for (int i = 0; i < dt.Rows.Count; i++)
            {
                bool isFindRoot = true;
                for (int j = 0; j < dt.Rows.Count; j++)
                {
                    if (i == j) continue;

                    if (dt.Rows[i][ColumnName.parentCatalog(format)].ToString()
                            .Equals(dt.Rows[j][ColumnName.catalog(format)].ToString()))
                    {
                        isFindRoot = false;
                        break;
                    }
                }
                if (isFindRoot) rootIndexes.Add(i);
            }
            #endregion

            ItemBom item = new ItemBom();
            //build roots
            foreach (int rootIndex in rootIndexes)
            {
                ItemBom ib = new ItemBom(dt.Rows[rootIndex][ColumnName.parentCatalog(format)].ToString());
                ib.name = dt.Rows[rootIndex][ColumnName.parentName(format)].ToString();
                bool newRoot = true;
                foreach (ItemBom ii in item.content)
                {
                    if (ii.catalog.Equals(ib.catalog))
                    {
                        newRoot = false;
                        break;
                    }
                }
                if (newRoot) 
                {
                    item.content.Add(ib);
                    ib.parent = item;//X
                }
            }

            //add subs
            while (dt.Rows.Count != 0)
            {
                List<ItemBom> itemBoms = new List<ItemBom>(item.content);
                for (int i = 0; i < itemBoms.Count; i++)
                {
                    for (int j = 0; j < dt.Rows.Count; j++)
                    {
                        if (itemBoms[i].catalog
                            .Equals(dt.Rows[j][ColumnName.parentCatalog(format)].ToString()))
                        {
                            //add Data
                            ItemBom add = new ItemBom();
                            add.catalog = dt.Rows[j][ColumnName.catalog(format)].ToString();
                            add.name = dt.Rows[j][ColumnName.name(format)].ToString();
                            add.specification = dt.Rows[j][ColumnName.specification(format)].ToString();
                            add.source = Source.P;
                            if (format == XlsxFormat.YjIt && dt.Rows[j][ColumnName.source(format)].ToString().Equals("U")) 
                                add.source = Source.U;
                            if (format == XlsxFormat.YjIt && dt.Rows[j][ColumnName.source(format)].ToString().Equals("M")) 
                                add.source = Source.M;
                            add.quantity = int.Parse(dt.Rows[j][ColumnName.quantity(format)].ToString());
                            add.remark = dt.Rows[j][ColumnName.remark(format)].ToString();

                            dt.Rows.RemoveAt(j);
                            j--;
                            itemBoms.Add(add);

                            //find root to add
                            rootIsFound = false;
                            findRoot(item.content, add);
                            add.parent = itemBoms[i];//X留,否則會被findRoot 蓋掉
                        }
                    }
                }
            }

            return item;
        }
        private static ItemBom readDB(DataTable dt)
        {
            ItemBom item = new ItemBom();
            foreach (DataRow dataRow in dt.Rows)
            {
                ItemBom add = new ItemBom(dataRow[ColumnName.catalog(XlsxFormat.DukeDB)].ToString()) 
                {
                    parent = item,//X
                    name = dataRow[ColumnName.name(XlsxFormat.DukeDB)].ToString(),
                    specification = dataRow[ColumnName.specification(XlsxFormat.DukeDB)].ToString()
                };
                item.content.Add(add);
            }

            return item;
        }

        private static bool rootIsFound = false;
        private static void findRoot(IList<ItemBom> list, ItemBom add)
        {
            foreach (ItemBom item in list)
            {
                if (add.parent.catalog.Equals(item.catalog))
                {
                    item.content.Add(add);
                    item.source = item.source == Source.M ? Source.M :Source.U;
                    rootIsFound = true;
                    return;
                }
                else
                {
                    findRoot(item.content, add);
                    if(rootIsFound) return;
                }
            }
        }
        public static void writeToDB(string path, ItemBom[] items, string[] columns)
        {
            if (path == null) return;
            List<ItemBom> list = new List<ItemBom>(items);

            #region follow path to write data, if has file to override it!
            ExcelManager manager = new ExcelManager(path);
            int result = manager.DataTableToExcel(
                covertSaveFormat(list.ToArray(), columns),
                null, true);//sheetName = null
            #endregion

            #region follow result show feedback
            string msg = "";
            if (result >= 0 && list.Count == result - 1)
            {
                msg = "Successful";
            }
            else if (result < 0)
            {
                msg = "Error!\nCode: " + result.ToString();
            }
            else
            {
                msg = string.Format("Error!\nwrite item not match!\n{0:d}/{0:d}", result - 1, list.Count);
            }
            MessageBox.Show(msg,
                "message", MessageBoxButton.OK,
                MessageBoxImage.Information
                );
            #endregion
        }
        private static DataTable covertSaveFormat(ItemBom[] items, string[] columns)
        {
            DataTable dataTable = new DataTable();

            #region columns add
            foreach (string col in columns)
            {
                dataTable.Columns.Add(col);
            }
            #endregion

            #region add data
            foreach (ItemBom item in items)
            {
                DataRow dataRow = dataTable.NewRow();
                dataRow[nameof(ItemBom.catalog)] = item.catalog;
                dataRow[nameof(ItemBom.name)] = item.name;
                dataRow[nameof(ItemBom.specification)] = item.specification;
                if (columns == Properties.bomSavePs)
                {
                    dataRow[nameof(ItemBom.parentName)] = (item as ItemBom).parentName;
                    dataRow[nameof(ItemBom.parentCatalog)] = (item as ItemBom).parentCatalog;
                    dataRow[nameof(ItemBom.quantity)] = (item as ItemBom).quantity;
                    dataRow[nameof(ItemBom.remark)] = (item as ItemBom).remark;
                }
                dataTable.Rows.Add(dataRow);
            }
            #endregion

            return dataTable;
        }
    }

    class ObservableRangeCollection<T> : ObservableCollection<T>
    {
        public delegate void CollectionChange(T item);
        private event CollectionChange onAddItem, onRemoveItem;

        public ObservableRangeCollection() : base() { }
        public ObservableRangeCollection(
            CollectionChange onAddItem, CollectionChange onRemoveItem
            ) : base() 
        {
            this.onAddItem += onAddItem;
            this.onRemoveItem += onRemoveItem;
        }

        public void AddRange(IEnumerable<T> collection)
        {
            if (collection == null)
                throw new ArgumentNullException("collection");

            foreach (var i in collection) Add(i);
        }
        public void InsertRange(int index, IEnumerable<T> collection)
        {
            if (index > Items.Count)
                throw new OverflowException(
                    "index: " + index + " >= Items.Count:" + Items.Count);
            else if (index == Items.Count)
                AddRange(collection);
            else
                for (int i = 0; i < collection.Count(); i++)
                    Insert(index + i, collection.ElementAt(i));
        }
        public void RemoveRange(IEnumerable<T> collection)
        {
            if (collection == null)
                throw new ArgumentNullException("collection");

            foreach (var i in collection)
                Items.Remove(i);
            OnCollectionChanged(
                new NotifyCollectionChangedEventArgs(
                    NotifyCollectionChangedAction.Reset));
        }
        public void RemoveRange(int index, int length)
        {
            if (index + length - 1 >= Items.Count)
                throw new OverflowException("length over");
            for (int i = length; i > 0; i--)
                Items.RemoveAt(index);
            OnCollectionChanged(
                new NotifyCollectionChangedEventArgs(
                    NotifyCollectionChangedAction.Reset));
        }
        public void Replace(T item)
        {
            ReplaceRange(new T[] { item });
        }
        public void ReplaceRange(IEnumerable<T> collection)
        {
            if (collection == null)
                throw new ArgumentNullException("collection");

            Items.Clear();
            foreach (var i in collection) Items.Add(i);
            OnCollectionChanged(
                new NotifyCollectionChangedEventArgs(
                    NotifyCollectionChangedAction.Reset));
        }
        public void MoveRange(int oldIndex, int length, int newIndex)
        {
            if (newIndex - oldIndex >= 0)
            {
                for (int i = length - 1; i >= 0; i--)
                {
                    Move(oldIndex + i, newIndex + i);
                }
            }
            else
            {
                for (int i = 0; i < length; i++)
                {
                    Move(oldIndex + i, newIndex + i);
                }

            }
        }
        public List<T> GetRange(int index, int length)
        {
            List<T> list = new List<T>();
            for (int i = index; i < index + length; i++)
            {
                list.Add(Items.ElementAt(i));
            }
            return list;
        }

        protected override void InsertItem(int index, T item)
        {
            base.InsertItem(index, item);
            onAddItem?.Invoke(item);
        }
        protected override void RemoveItem(int index)
        {
            onRemoveItem?.Invoke(Items.ElementAt(index));
            base.RemoveItem(index);
        }
    }

    /// <summary>
    /// This is not expended from HashSet!
    /// </summary>
    /// <typeparam name="T"></typeparam>
    class ObservableHashSet<T> : ObservableCollection<T>
    {
        public delegate void OnSetChange();
        private event OnSetChange onSetChange;

        public ObservableHashSet(OnSetChange onSetChange)
        {
            this.onSetChange += onSetChange;
        }

        protected override void InsertItem(int index, T item)
        {
            if (!Contains(item)) 
            {
                base.InsertItem(index, item);
                onSetChange?.Invoke();
            }
        }
        protected override void SetItem(int index, T item)
        {
            int i = IndexOf(item);
            if (i >= 0 && i != index) 
            {
                base.SetItem(index, item);
                onSetChange?.Invoke();
            }
        }
    }
    
    interface IUIProperty
    {
        #region UI control properties
        bool isExpanded { get; set; }
        int FontSize { get; }
        Brush RowHeaderBachGroud { get; }

        #endregion
    }


    /*my notes
     * ItemBom子項目事件,//X部分
     * Bom.cs
     * WindowBomCreate
     * WindowBomItem
     * 
     * 
     * //X 不能蓋
     * MotorSupport
     * BomItem
     * 
     * 
     * MotorSupport 有機會做個電壓換算
     * 
     * 
     */
}
