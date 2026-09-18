import React, { useEffect, useState, useCallback } from "react";
import { warehouseApi } from "../api/warehouseApi";
import { inventoryApi } from "../api/inventoryApi";
import { analyticsApi } from "../api/analyticsApi";
import { Badge } from "../components/common/Badge";
import { LoadingPage } from "../components/common/Spinner";
import { Modal } from "../components/common/Modal";
import { useToast } from "../components/common/Toast";
import {
  Building2,
  Package,
  TrendingDown,
  TrendingUp,
  SlidersHorizontal,
  AlertTriangle,
  RefreshCw,
  Boxes,
  Download,
  FileSpreadsheet
} from "lucide-react";

const StatBox = ({ label, value, sub, color = "var(--accent-primary)" }) => (
  <div style={{ background: "rgba(255,255,255,0.04)", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "14px 18px", minWidth: 110, flex: "1 1 110px" }}>
    <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: ".05em" }}>{label}</div>
    <div style={{ fontSize: "1.5rem", fontWeight: 700, color, lineHeight: 1.2, marginTop: 4 }}>{value}</div>
    {sub && <div style={{ fontSize: "0.72rem", color: "var(--text-muted)", marginTop: 2 }}>{sub}</div>}
  </div>
);

const StockModal = ({ mode, item, onClose, onSuccess }) => {
  const { addToast } = useToast();
  const [form, setForm] = useState({ warehouseId: item?.warehouseId || "", productId: item?.productId || "", quantity: "", newQuantity: "", notes: "" });
  const [busy, setBusy] = useState(false);
  const titles = { in: "Stock In", out: "Stock Out", adjust: "Adjust Stock" };
  const handleSubmit = async () => {
    const qty = mode === "adjust" ? parseInt(form.newQuantity) : parseInt(form.quantity);
    if (!qty || qty < 1) { addToast("Enter a valid quantity", "warning"); return; }
    setBusy(true);
    try {
      if (mode === "in") { await inventoryApi.stockIn({ productId: form.productId, warehouseId: form.warehouseId, quantity: qty, notes: form.notes }); addToast("Stock added successfully", "success"); }
      else if (mode === "out") { await inventoryApi.stockOut({ productId: form.productId, warehouseId: form.warehouseId, quantity: qty, notes: form.notes }); addToast("Stock removed successfully", "success"); }
      else { await inventoryApi.adjustStock({ productId: form.productId, warehouseId: form.warehouseId, newQuantity: qty, notes: form.notes }); addToast("Stock adjusted successfully", "success"); }
      onSuccess(); onClose();
    } catch (e) { addToast(e?.response?.data?.message || "Operation failed", "danger"); }
    finally { setBusy(false); }
  };
  return (
    <Modal isOpen onClose={onClose} title={`${titles[mode]}: ${item?.productName || ""}`} footer={<><button className="btn btn-secondary" onClick={onClose} disabled={busy}>Cancel</button><button className="btn btn-primary" onClick={handleSubmit} disabled={busy}>{busy ? "Processing..." : titles[mode]}</button></>}>
      <div>
        <div style={{ padding: "12px 16px", background: "rgba(255,255,255,0.04)", borderRadius: "var(--radius-md)", marginBottom: 16, fontSize: ".875rem" }}>
          <div style={{ display: "flex", gap: 24, flexWrap: "wrap" }}>
            <span><span style={{ color: "var(--text-secondary)" }}>SKU: </span><strong>{item?.productSku}</strong></span>
            <span><span style={{ color: "var(--text-secondary)" }}>Warehouse: </span><strong>{item?.warehouseName}</strong></span>
            <span><span style={{ color: "var(--text-secondary)" }}>On-Hand: </span><strong>{item?.quantity}</strong></span>
            <span><span style={{ color: "var(--text-secondary)" }}>Available: </span><strong>{item?.availableQuantity}</strong></span>
          </div>
        </div>
        {mode === "adjust" ? (
          <div className="form-group"><label className="form-label">New Quantity</label><input type="number" min="0" className="form-input" placeholder="Enter target quantity" value={form.newQuantity} onChange={e => setForm({ ...form, newQuantity: e.target.value })} /></div>
        ) : (
          <div className="form-group"><label className="form-label">{mode === "in" ? "Qty to Add" : "Qty to Remove"}</label><input type="number" min="1" className="form-input" placeholder={mode === "in" ? "Units to receive" : "Units to issue"} value={form.quantity} onChange={e => setForm({ ...form, quantity: e.target.value })} /></div>
        )}
        <div className="form-group"><label className="form-label">Notes (optional)</label><input type="text" className="form-input" placeholder="Reason / reference" value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} /></div>
      </div>
    </Modal>
  );
};

export const Inventory = () => {
  const { addToast } = useToast();
  const [warehouses, setWarehouses] = useState([]);
  const [stockByWarehouse, setStockByWarehouse] = useState({});
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [activeModal, setActiveModal] = useState(null);

  const loadWarehouses = useCallback(async () => {
    try {
      const res = await warehouseApi.getWarehouses();
      const list = Array.isArray(res) ? res : (res?.content ?? []);
      setWarehouses(list);
      return list;
    } catch (err) {
      console.error("Failed to load warehouses", err);
      addToast("Could not load warehouses", "danger");
      return [];
    }
  }, [addToast]);

  const loadInventory = useCallback(async (whList) => {
    const map = {};
    await Promise.all(whList.map(async (wh) => {
      try {
        const res = await inventoryApi.getWarehouseInventory(wh.id);
        map[wh.id] = Array.isArray(res) ? res : (res?.content ?? []);
      } catch (err) {
        console.error("Failed to load inventory for WH " + wh.id, err);
        map[wh.id] = [];
      }
    }));
    setStockByWarehouse(map);
  }, []);

  useEffect(() => {
    const init = async () => {
      setLoading(true);
      const whList = await loadWarehouses();
      if (whList.length > 0) await loadInventory(whList);
      setLoading(false);
    };
    init();
  }, [loadWarehouses, loadInventory]);

  const handleRefresh = async () => {
    setRefreshing(true);
    const whList = await loadWarehouses();
    if (whList.length > 0) await loadInventory(whList);
    setRefreshing(false);
    addToast("Stock data refreshed", "success");
  };

  const allItems = Object.values(stockByWarehouse).flat();
  const totalUnits = allItems.reduce((s, i) => s + (i.quantity ?? 0), 0);
  const totalAvailable = allItems.reduce((s, i) => s + (i.availableQuantity ?? 0), 0);
  const totalReserved = allItems.reduce((s, i) => s + (i.reservedQuantity ?? 0), 0);
  const lowStockCount = allItems.filter(i => i.isLowStock).length;

  if (loading) return <LoadingPage />;

  return (
    <div>
      <div className="page-header" style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: 16 }}>
        <div>
          <h1 className="page-title">Multi-Warehouse Stock</h1>
          <p className="page-subtitle">Live inventory across {warehouses.length} warehouses &mdash; receive, issue &amp; adjust stock</p>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button className="btn btn-secondary btn-sm" onClick={() => analyticsApi.downloadLowStockCsv()} style={{ display: "flex", alignItems: "center", gap: 6, color: "#f59e0b" }}>
            <Download size={14} /> Export Low Stock CSV
          </button>
          <button className="btn btn-secondary btn-sm" onClick={() => analyticsApi.downloadInventoryCsv()} style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <FileSpreadsheet size={14} /> Export All Inventory CSV
          </button>
          <button className="btn btn-secondary btn-sm" onClick={handleRefresh} disabled={refreshing} style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <RefreshCw size={14} />{refreshing ? "Refreshing..." : "Refresh"}
          </button>
        </div>
      </div>

      <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginBottom: 24 }}>
        <StatBox label="Total On-Hand" value={totalUnits.toLocaleString()} color="var(--accent-primary)" />
        <StatBox label="Available" value={totalAvailable.toLocaleString()} color="var(--success)" />
        <StatBox label="Reserved" value={totalReserved.toLocaleString()} color="var(--warning)" />
        <StatBox label="Low Stock Items" value={lowStockCount} color={lowStockCount > 0 ? "var(--danger)" : "var(--success)"} sub={lowStockCount > 0 ? "Action needed" : "All levels OK"} />
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(520px, 1fr))", gap: 20 }}>
        {warehouses.map((wh) => {
          const items = stockByWarehouse[wh.id] ?? [];
          const whTotal = items.reduce((s, i) => s + (i.quantity ?? 0), 0);
          const whAvail = items.reduce((s, i) => s + (i.availableQuantity ?? 0), 0);
          const whLow = items.filter(i => i.isLowStock).length;
          return (
            <div key={wh.id} className="glass-card" style={{ padding: 0, overflow: "hidden" }}>
              <div style={{ padding: "16px 20px", background: "linear-gradient(135deg, rgba(99,102,241,0.15), rgba(79,70,229,0.05))", borderBottom: "1px solid var(--border-color)", display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 8 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <Building2 size={20} color="var(--accent-primary)" />
                  <div>
                    <div style={{ fontWeight: 700, fontSize: "1rem" }}>{wh.name}</div>
                    <div style={{ fontSize: "0.75rem", color: "var(--text-muted)" }}>{wh.code} &middot; {wh.city}, {wh.state}, {wh.country}</div>
                  </div>
                </div>
                <div style={{ display: "flex", gap: 8, flexWrap: "wrap", fontSize: "0.78rem", alignItems: "center" }}>
                  <span style={{ padding: "3px 8px", background: "rgba(99,102,241,0.15)", borderRadius: 20, color: "var(--accent-primary)" }}>
                    <Boxes size={11} style={{ marginRight: 4, verticalAlign: "middle" }} />{whTotal.toLocaleString()} units
                  </span>
                  <span style={{ padding: "3px 8px", background: "rgba(16,185,129,0.12)", borderRadius: 20, color: "var(--success)" }}>{whAvail.toLocaleString()} avail</span>
                  {whLow > 0 && <span style={{ padding: "3px 8px", background: "rgba(239,68,68,0.12)", borderRadius: 20, color: "var(--danger)" }}><AlertTriangle size={11} style={{ marginRight: 4, verticalAlign: "middle" }} />{whLow} low</span>}
                  <Badge status={wh.status || "ACTIVE"} />
                </div>
              </div>

              {items.length === 0 ? (
                <div style={{ padding: 40, textAlign: "center", color: "var(--text-muted)" }}>
                  <Package size={32} style={{ opacity: 0.3, marginBottom: 8 }} />
                  <p>No inventory records in this warehouse.</p>
                </div>
              ) : (
                <div style={{ overflowX: "auto" }}>
                  <table className="data-table" style={{ margin: 0 }}>
                    <thead>
                      <tr>
                        <th>Product</th>
                        <th style={{ textAlign: "right" }}>On-Hand</th>
                        <th style={{ textAlign: "right" }}>Reserved</th>
                        <th style={{ textAlign: "right" }}>Available</th>
                        <th style={{ textAlign: "right" }}>Reorder</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {items.map((item) => (
                        <tr key={item.id} style={item.isLowStock ? { background: "rgba(239,68,68,0.04)" } : {}}>
                          <td>
                            <div style={{ fontWeight: 600, fontSize: "0.875rem" }}>{item.productName}</div>
                            <div style={{ fontSize: "0.72rem", color: "var(--accent-primary)", fontFamily: "monospace" }}>{item.productSku}</div>
                          </td>
                          <td style={{ textAlign: "right", fontWeight: 700 }}>{(item.quantity ?? 0).toLocaleString()}</td>
                          <td style={{ textAlign: "right", color: "var(--warning)" }}>{(item.reservedQuantity ?? 0).toLocaleString()}</td>
                          <td style={{ textAlign: "right", fontWeight: 700, color: "var(--success)" }}>{(item.availableQuantity ?? 0).toLocaleString()}</td>
                          <td style={{ textAlign: "right", color: "var(--text-muted)", fontSize: "0.8rem" }}>{item.reorderLevel ?? "-"}</td>
                          <td>
                            {item.isLowStock
                              ? <span style={{ fontSize: "0.72rem", padding: "2px 8px", borderRadius: 20, background: "rgba(239,68,68,0.15)", color: "var(--danger)", fontWeight: 700 }}>LOW STOCK</span>
                              : <Badge status="IN_STOCK" />}
                          </td>
                          <td>
                            <div style={{ display: "flex", gap: 4 }}>
                              <button title="Stock In" onClick={() => setActiveModal({ mode: "in", item })} style={{ padding: "4px 8px", background: "rgba(16,185,129,0.15)", color: "var(--success)", border: "none", borderRadius: 6, cursor: "pointer" }}><TrendingUp size={13} /></button>
                              <button title="Stock Out" onClick={() => setActiveModal({ mode: "out", item })} style={{ padding: "4px 8px", background: "rgba(239,68,68,0.12)", color: "var(--danger)", border: "none", borderRadius: 6, cursor: "pointer" }}><TrendingDown size={13} /></button>
                              <button title="Adjust Stock" onClick={() => setActiveModal({ mode: "adjust", item })} style={{ padding: "4px 8px", background: "rgba(245,158,11,0.12)", color: "var(--warning)", border: "none", borderRadius: 6, cursor: "pointer" }}><SlidersHorizontal size={13} /></button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {activeModal && (
        <StockModal mode={activeModal.mode} item={activeModal.item} warehouses={warehouses} onClose={() => setActiveModal(null)} onSuccess={handleRefresh} />
      )}
    </div>
  );
};
