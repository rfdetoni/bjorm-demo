"use strict";
const $ = id => document.getElementById(id);
const money = value => new Intl.NumberFormat('pt-BR', {style: 'currency', currency: 'BRL'}).format(Number(value || 0));
const number = value => new Intl.NumberFormat('pt-BR', {maximumFractionDigits: 2}).format(value);
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
let page = 0, pageCount = 0, fetchSequence = 0, debounce, toastTimer;

function toast(message, error = false) {
  const el = $('toast'); el.textContent = message; el.classList.toggle('error', error); el.hidden = false;
  clearTimeout(toastTimer); toastTimer = setTimeout(() => el.hidden = true, 5500);
}
async function api(path, opts = {}) {
  const since = performance.now();
  const response = await fetch(path, {headers: {'Content-Type': 'application/json'}, ...opts});
  $('stat-latency').textContent = number(performance.now() - since);
  if (!response.ok) {
    let error; try {error = await response.json();} catch {error = {};}
    throw new Error(error.detail || `Erro HTTP ${response.status}`);
  }
  if (response.status === 204) return null;
  return response.json();
}
function resetForm() {
  $('product-form').reset(); $('product-id').value = ''; $('product-version').value = '';
  $('form-title').textContent = 'Novo produto'; $('price').value = '19.90'; $('stock').value = '10'; $('active').checked = true;
}
async function loadProducts() {
  const sequence = ++fetchSequence;
  const params = new URLSearchParams({page, size: $('size').value, search: $('search').value,
    sort: $('sort').value, descending: $('direction').value === 'desc', compact: $('compact').checked});
  try {
    const data = await api(`/api/products?${params}`);
    if (sequence !== fetchSequence) return;
    pageCount = data.pages;
    $('stat-count').textContent = number(data.total);
    $('page-description').textContent = data.total ? `${page * data.size + 1}–${Math.min((page + 1) * data.size, data.total)} de ${number(data.total)}` : 'Nenhum registro';
    $('page-number').textContent = `${page + 1} / ${Math.max(1, pageCount)}`;
    $('previous').disabled = page === 0; $('next').disabled = page >= pageCount - 1;
    const compact = $('compact').checked;
    $('products').innerHTML = data.items.length ? data.items.map(item => `<tr>
      <td class="product-name" title="${escapeHtml(item.name)}">${escapeHtml(item.name)}${compact ? '' : `<small class="subtext">${escapeHtml(item.description || 'Sem descrição')}</small>`}</td>
      <td>${money(item.price)}</td><td>${number(item.stock)}</td>
      <td>${compact ? '<span class="muted">—</span>' : `<span class="tag ${item.active ? '' : 'off'}">${item.active ? 'Ativo' : 'Inativo'}</span>`}</td>
      <td>${number(item.version)}</td>
      <td class="actions-col"><div class="table-actions"><button type="button" class="icon-btn" data-edit="${escapeHtml(item.id)}" aria-label="Editar ${escapeHtml(item.name)}">Editar</button><button type="button" class="icon-btn danger" data-delete="${escapeHtml(item.id)}" data-version="${item.version}" aria-label="Excluir ${escapeHtml(item.name)}">Excluir</button></div></td></tr>`).join('') : '<tr><td colspan="6" class="empty">Nenhum produto. Cadastre um ou gere dados de teste.</td></tr>';
  } catch(e) { if (sequence === fetchSequence) { toast(e.message, true); $('products').innerHTML = '<tr><td colspan="6" class="empty">Erro ao carregar produtos</td></tr>'; } }
}
async function saveProduct(event) {
  event.preventDefault();
  const form = $('product-form'); if (!form.reportValidity()) return;
  const id = $('product-id').value;
  const body = {name: $('name').value.trim(), description: $('description').value || null,
    price: Number($('price').value), stock: Number($('stock').value), active: $('active').checked,
    version: id ? Number($('product-version').value) : null};
  const button = $('save'); button.disabled = true;
  try {
    await api(id ? `/api/products/${id}` : '/api/products', {method: id ? 'PUT' : 'POST', body: JSON.stringify(body)});
    toast(id ? 'Produto atualizado com sucesso' : 'Produto criado com sucesso');
    resetForm(); page = 0; await loadProducts();
  } catch(e) { toast(e.message, true); if (e.message.includes('alterado')) await loadProducts(); }
  finally {button.disabled = false;}
}
async function tableAction(event) {
  const edit = event.target.closest('[data-edit]');
  const del = event.target.closest('[data-delete]');
  if (edit) {
    try {
      const item = await api(`/api/products/${encodeURIComponent(edit.dataset.edit)}`);
      $('product-id').value = item.id; $('product-version').value = item.version;
      $('name').value = item.name; $('description').value = item.description || '';
      $('price').value = item.price; $('stock').value = item.stock; $('active').checked = item.active;
      $('form-title').textContent = 'Editar produto'; $('name').focus();
    } catch(e) {toast(e.message, true);}
  }
  if (del) {
    if (!window.confirm('Excluir este produto? Esta operação não pode ser desfeita.')) return;
    try {
      await api(`/api/products/${encodeURIComponent(del.dataset.delete)}?version=${encodeURIComponent(del.dataset.version)}`, {method:'DELETE'});
      toast('Produto excluído'); await loadProducts();
    } catch(e) {toast(e.message, true);}
  }
}
async function seedData() {
  const button = $('seed-button'); button.disabled = true;
  try { const result = await api('/api/products/seed?count=250', {method:'POST'});
    toast(`${result.inserted} produtos inseridos em lote via BJORM`); page = 0; await loadProducts();
  } catch(e) {toast(e.message,true);} finally {button.disabled = false;}
}
function cell(text) {return `<td>${escapeHtml(text)}</td>`;}
async function runBenchmark() {
  const request = {iterations: Number($('iterations').value), warmup: Number($('warmup').value), concurrency: Number($('concurrency').value)};
  if (!Number.isInteger(request.iterations) || request.iterations < 10 || request.iterations > 500 ||
      !Number.isInteger(request.warmup) || request.warmup < 0 || request.warmup > 100) {toast('Parâmetros fora dos limites.',true);return;}
  const button = $('run-benchmark'); button.disabled = true; button.textContent = 'Executando…';
  $('benchmark-results').hidden = true; $('benchmark-message').textContent = 'Executando comparação ABBA no PostgreSQL (BJORM / JDBC / JDBC / BJORM)…';
  try {
    const data = await api('/api/benchmark', {method:'POST', body:JSON.stringify(request)});
    const bjorm = data.find(x => x.engine === 'BJORM'); const jdbc = data.find(x => x.engine === 'JDBC');
    if (!bjorm || !jdbc) throw new Error('Resposta do benchmark incompleta');
    $('bjorm-p50').textContent = number(bjorm.p50Ms); $('jdbc-p50').textContent = number(jdbc.p50Ms);
    $('bjorm-ops').textContent = `${number(bjorm.throughputPerSecond)} tx/s`;
    $('jdbc-ops').textContent = `${number(jdbc.throughputPerSecond)} tx/s`;
    $('benchmark-table').innerHTML = data.map(x => `<tr>${cell(x.engine)}${cell(x.transactions)}${cell(x.avgMs + ' ms')}${cell(x.p50Ms + ' ms')}${cell(x.p95Ms + ' ms')}${cell(x.p99Ms + ' ms')}${cell(x.maxMs + ' ms')}${cell(number(x.throughputPerSecond))}</tr>`).join('');
    $('benchmark-message').textContent = `Coletadas ${bjorm.transactions} amostras por implementação. Veja as métricas servidor abaixo.`;
    $('benchmark-results').hidden = false;
    await loadProducts();
  } catch(e) { $('benchmark-message').textContent = 'Não foi possível concluir a medição.'; toast(e.message,true); }
  finally {button.disabled = false; button.textContent = '▶ Executar comparação';}
}
$('product-form').addEventListener('submit', saveProduct);
$('reset').addEventListener('click', resetForm);
$('seed-button').addEventListener('click', seedData);
$('products').addEventListener('click', tableAction);
$('run-benchmark').addEventListener('click', runBenchmark);
$('previous').addEventListener('click', () => { if (page > 0) {page--; loadProducts();} });
$('next').addEventListener('click', () => { if (page < pageCount - 1) {page++; loadProducts();} });
for (const id of ['sort','direction','size','compact']) $(id).addEventListener('change', () => {page=0;loadProducts();});
$('search').addEventListener('input', () => {clearTimeout(debounce);debounce=setTimeout(() => {page=0;loadProducts();},280);});
resetForm(); loadProducts();
