const statusGrid = document.getElementById("status-grid");
const summary = document.getElementById("summary");
const flights = document.getElementById("flights");
const invariants = document.getElementById("invariants");
const petri = document.getElementById("petri");
const events = document.getElementById("events");
const toast = document.getElementById("toast");

const schemaEnVol = document.getElementById("schema-en-vol");
const schemaApproche = document.getElementById("schema-approche");
const schemaPiste = document.getElementById("schema-piste");
const schemaParking = document.getElementById("schema-parking");

function badgeClass(value) {
  return value ? "good" : "bad";
}

function labelClass(text) {
  const value = String(text).toLowerCase();
  if (value.includes("autorisé") || value.includes("libre") || value.includes("ok") || value.includes("bonne")) return "good";
  if (value.includes("mauvaise") || value.includes("aucune")) return "warn";
  if (value.includes("occupée") || value.includes("perdue") || value.includes("critique") || value.includes("intrusion") || value.includes("panne")) return "bad";
  return "warn";
}

function showToast(message, ok = true) {
  if (!toast) return;

  toast.textContent = message;
  toast.className = `toast ${ok ? "" : "bad"}`;
  setTimeout(() => {
    toast.className = "toast hidden";
  }, 2800);
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    method: options.method || "GET",
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const data = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    throw new Error(typeof data === "string" ? data : data.message || "Erreur API");
  }

  return data;
}

function detectFlightZone(flight) {
  const status = String(flight.status || "").toLowerCase();
  const operation = String(flight.lastOperation || "").toLowerCase();

  if (status.includes("en vol")) return "en-vol";
  if (status.includes("approche")) return "approche";
  if (status.includes("au parking")) return "parking";
  if (status.includes("au sol")) return "parking";
  if (status.includes("autorisé")) return "piste";

  if (operation.includes("décollage") || operation.includes("decollage")) {
    if (status.includes("attente")) return "parking";
  }

  if (operation.includes("atterrissage")) {
    if (status.includes("attente")) return "approche";
  }

  if (flight.id === "AF447") return "parking";
  if (flight.id === "AF112") return "approche";

  return "parking";
}

function createSchemaPlane(flight) {
  const badge = flight.technicalOk ? "OK" : "PANNE";

  const plane = document.createElement("div");
  plane.className = `schema-plane ${flight.technicalOk ? "" : "schema-plane-bad"}`.trim();
  plane.innerHTML = `
    <div class="schema-plane-code">✈ ${flight.id}</div>
    <div class="schema-plane-meta">${badge}</div>
  `;
  return plane;
}

function renderSchema(state) {
  if (!schemaEnVol || !schemaApproche || !schemaPiste || !schemaParking) return;

  schemaEnVol.innerHTML = "";
  schemaApproche.innerHTML = "";
  schemaPiste.innerHTML = "";
  schemaParking.innerHTML = "";

  state.flights.forEach((flight) => {
    const plane = createSchemaPlane(flight);
    const zone = detectFlightZone(flight);

    if (zone === "en-vol") schemaEnVol.appendChild(plane);
    else if (zone === "approche") schemaApproche.appendChild(plane);
    else if (zone === "piste") schemaPiste.appendChild(plane);
    else schemaParking.appendChild(plane);
  });

  [schemaEnVol, schemaApproche, schemaPiste, schemaParking].forEach((container) => {
    if (!container.children.length) {
      const empty = document.createElement("div");
      empty.className = "schema-empty";
      empty.textContent = "—";
      container.appendChild(empty);
    }
  });
}

function renderState(state) {
  const items = [
    ["Météo", state.weather],
    ["Piste", state.runwayFree ? "Libre" : "Occupée"],
    ["Communication", state.communicationOk ? "OK" : "Perdue"],
    ["Intrusion", state.intrusionDetected ? "Détectée" : "Aucune"],
    ["Conflit d'approche", state.nearbyAircraft ? "Oui" : "Non"],
    ["Opération active", state.activeOperation],
  ];

  if (statusGrid) {
    statusGrid.innerHTML = items
        .map(([label, value]) => `
        <article class="stat-card">
          <strong>${label}</strong>
          <div class="stat-value ${labelClass(value)}">${value}</div>
        </article>
      `)
        .join("");
  }

  if (summary) {
    summary.textContent = state.summary || "";
  }

  renderSchema(state);

  if (flights) {
    flights.innerHTML = state.flights
        .map(
            (flight) => `
      <article class="flight-card">
        <div style="display:flex;justify-content:space-between;gap:1rem;align-items:center;">
          <h3>${flight.id}</h3>
          <span class="badge ${flight.technicalOk ? "good" : "bad"}">
            ${flight.technicalOk ? "Technique OK" : "Panne"}
          </span>
        </div>
        <p><strong>Statut :</strong> ${flight.status || "-"}</p>
        <p><strong>Dernière opération :</strong> ${flight.lastOperation || "-"}</p>
        <p><strong>Dernière décision :</strong> ${flight.lastDecision || "-"}</p>
      </article>
    `
        )
        .join("");
  }

  if (invariants) {
    invariants.innerHTML = state.invariants
        .map(
            (item) => `
        <article class="invariant">
          <div style="display:flex;justify-content:space-between;gap:1rem;align-items:center;">
            <strong>${item.id} - ${item.description}</strong>
            <span class="badge ${item.passed ? "good" : "bad"}">${item.passed ? "OK" : "KO"}</span>
          </div>
          <p>${item.details}</p>
        </article>
      `
        )
        .join("");
  }

  if (petri) {
    petri.innerHTML = state.petriViews
        .map(
            (view) => `
        <article class="petri-card">
          <strong>${view.operation}</strong>
          <p><strong>Transitions activées :</strong> ${view.enabledTransitions.length ? view.enabledTransitions.join(", ") : "aucune"}</p>
          <p><strong>Deadlock :</strong> ${view.deadlock ? "oui" : "non"}</p>
        </article>
      `
        )
        .join("");
  }

  if (events) {
    events.innerHTML = state.recentEvents
        .map((event) => `<div class="event">${event}</div>`)
        .join("");
  }
}

async function refresh() {
  const state = await api("/api/state");
  renderState(state);
}

async function sendAndRefresh(path, method = "POST") {
  const result = await api(path, { method });
  renderState(result.state);
  showToast(result.message, result.ok);
}

document.querySelector('[data-action="reset"]').addEventListener("click", () => sendAndRefresh("/api/reset"));
document.querySelector('[data-action="refresh"]').addEventListener("click", refresh);
document.querySelector('[data-action="complete"]').addEventListener("click", () => sendAndRefresh("/api/complete"));

document.querySelectorAll("[data-request]").forEach((button) => {
  button.addEventListener("click", () => {
    const [flightId, operation] = button.dataset.request.split(":");
    sendAndRefresh(`/api/request/${flightId}/${operation}`);
  });
});

document.querySelectorAll("[data-weather]").forEach((button) => {
  button.addEventListener("click", () => sendAndRefresh(`/api/weather/${button.dataset.weather}`));
});

document.querySelectorAll("[data-runway]").forEach((button) => {
  button.addEventListener("click", () => sendAndRefresh(`/api/runway/${button.dataset.runway}`));
});

document.querySelectorAll("[data-communication]").forEach((button) => {
  button.addEventListener("click", () => sendAndRefresh(`/api/communication/${button.dataset.communication}`));
});

document.querySelectorAll("[data-intrusion]").forEach((button) => {
  button.addEventListener("click", () => sendAndRefresh(`/api/intrusion/${button.dataset.intrusion}`));
});

document.querySelectorAll("[data-nearby]").forEach((button) => {
  button.addEventListener("click", () => sendAndRefresh(`/api/nearby/${button.dataset.nearby}`));
});

document.querySelectorAll("[data-technical]").forEach((button) => {
  button.addEventListener("click", () => {
    const [flightId, mode] = button.dataset.technical.split(":");
    sendAndRefresh(`/api/technical/${flightId}/${mode}`);
  });
});

refresh().catch((error) => showToast(error.message, false));
setInterval(() => refresh().catch(() => undefined), 5000);