const state = {
    listings: [],
    activeListing: null,
    token: localStorage.getItem("nido.token"),
    user: JSON.parse(localStorage.getItem("nido.user") || "null"),
    authMode: "login"
};

const elements = {
    listingGrid: document.querySelector("#listingGrid"),
    emptyState: document.querySelector("#emptyState"),
    resultCount: document.querySelector("#resultCount"),
    searchForm: document.querySelector("#searchForm"),
    listingDialog: document.querySelector("#listingDialog"),
    authDialog: document.querySelector("#authDialog"),
    authForm: document.querySelector("#authForm"),
    tripsDialog: document.querySelector("#tripsDialog"),
    hostDialog: document.querySelector("#hostDialog"),
    toast: document.querySelector("#toast")
};

const escapeHtml = value => String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");

const money = value => new Intl.NumberFormat("en-GB", {
    style: "currency",
    currency: "EUR",
    maximumFractionDigits: 0
}).format(value);

const shortDate = value => new Intl.DateTimeFormat("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric"
}).format(new Date(`${value}T12:00:00`));

async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body) {
        headers.set("Content-Type", "application/json");
    }
    if (state.token) {
        headers.set("Authorization", `Bearer ${state.token}`);
    }
    const response = await fetch(path, {...options, headers});
    if (response.status === 204) {
        return null;
    }
    const body = await response.json().catch(() => null);
    if (!response.ok) {
        if (response.status === 401 && state.token) {
            clearSession();
        }
        const fieldMessage = body?.fieldErrors
            ? Object.values(body.fieldErrors)[0]
            : null;
        throw new Error(fieldMessage || body?.message || "Something went wrong");
    }
    return body;
}

function showToast(message, error = false) {
    elements.toast.textContent = message;
    elements.toast.classList.toggle("error", error);
    elements.toast.classList.add("show");
    window.setTimeout(() => elements.toast.classList.remove("show"), 3200);
}

function setSession(auth) {
    state.token = auth.token;
    state.user = {
        id: auth.userId,
        name: auth.name,
        email: auth.email,
        role: auth.role
    };
    localStorage.setItem("nido.token", state.token);
    localStorage.setItem("nido.user", JSON.stringify(state.user));
    updateNavigation();
}

function clearSession() {
    state.token = null;
    state.user = null;
    localStorage.removeItem("nido.token");
    localStorage.removeItem("nido.user");
    updateNavigation();
}

function updateNavigation() {
    const loggedIn = Boolean(state.user);
    document.querySelector("#loginButton").hidden = loggedIn;
    document.querySelector("#registerButton").hidden = loggedIn;
    document.querySelector("#tripsButton").hidden = !loggedIn;
    document.querySelector("#hostButton").hidden = state.user?.role !== "HOST";
    const accountButton = document.querySelector("#accountButton");
    accountButton.hidden = !loggedIn;
    accountButton.textContent = loggedIn ? state.user.name.charAt(0).toUpperCase() : "";
    accountButton.title = loggedIn ? `${state.user.name} · Log out` : "";
}

async function loadListings(params = new URLSearchParams()) {
    elements.listingGrid.innerHTML = `
        <div class="loading-card"></div>
        <div class="loading-card"></div>
        <div class="loading-card"></div>
    `;
    elements.emptyState.hidden = true;
    try {
        state.listings = await request(`/api/listings?${params.toString()}`);
        renderListings();
    } catch (error) {
        elements.listingGrid.innerHTML = "";
        elements.emptyState.hidden = false;
        showToast(error.message, true);
    }
}

function renderListings() {
    elements.resultCount.textContent = `${state.listings.length} ${state.listings.length === 1 ? "stay" : "stays"}`;
    elements.emptyState.hidden = state.listings.length > 0;
    elements.listingGrid.innerHTML = state.listings.map(listing => `
        <article class="listing-card">
            <button type="button" data-listing-id="${listing.id}" aria-label="View ${escapeHtml(listing.title)}">
                <div class="listing-image-wrap">
                    <img class="listing-image" src="${escapeHtml(listing.imageUrl)}" alt="${escapeHtml(listing.title)}">
                </div>
                <div class="listing-card-body">
                    <div class="listing-card-top">
                        <h3>${escapeHtml(listing.title)}</h3>
                        <span class="listing-rating">${listing.averageRating ? `★ ${listing.averageRating}` : "New"}</span>
                    </div>
                    <p class="listing-location">${escapeHtml(listing.city)}, ${escapeHtml(listing.country)} · ${listing.maxGuests} guests</p>
                    <p class="listing-price"><strong>${money(listing.nightlyPrice)}</strong> per night</p>
                </div>
            </button>
        </article>
    `).join("");
}

async function openListing(id) {
    try {
        const listing = state.listings.find(item => item.id === Number(id))
            || await request(`/api/listings/${id}`);
        state.activeListing = listing;
        document.querySelector("#detailImage").src = listing.imageUrl;
        document.querySelector("#detailImage").alt = listing.title;
        document.querySelector("#detailLocation").textContent = `${listing.city}, ${listing.country}`;
        document.querySelector("#detailTitle").textContent = listing.title;
        document.querySelector("#detailRating").textContent = listing.averageRating
            ? `★ ${listing.averageRating} · ${listing.reviewCount}`
            : "New stay";
        document.querySelector("#detailMeta").textContent =
            `${listing.propertyType.toLowerCase()} · ${listing.maxGuests} guests · ${listing.bedrooms} bedrooms · ${listing.beds} beds`;
        document.querySelector("#detailDescription").textContent = listing.description;
        document.querySelector("#detailPrice").textContent = money(listing.nightlyPrice);
        document.querySelector("#detailAmenities").innerHTML = listing.amenities
            .map(amenity => `<span class="amenity">${escapeHtml(amenity)}</span>`)
            .join("");
        const bookingForm = document.querySelector("#bookingForm");
        bookingForm.elements.listingId.value = listing.id;
        bookingForm.elements.guests.max = listing.maxGuests;
        await loadReviews(listing.id);
        elements.listingDialog.showModal();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function loadReviews(listingId) {
    const reviewList = document.querySelector("#reviewList");
    const reviews = await request(`/api/listings/${listingId}/reviews`);
    reviewList.innerHTML = reviews.length
        ? reviews.map(review => `
            <article class="review-item">
                <strong>${escapeHtml(review.authorName)} · ${"★".repeat(review.rating)}</strong>
                <p>${escapeHtml(review.comment)}</p>
            </article>
        `).join("")
        : "<p>No guest notes yet.</p>";
}

function openAuth(mode) {
    state.authMode = mode;
    const registering = mode === "register";
    elements.authDialog.classList.toggle("register-mode", registering);
    document.querySelector("#authEyebrow").textContent = registering ? "A place for every trip" : "Welcome back";
    document.querySelector("#authTitle").textContent = registering ? "Create your account" : "Log in to Nido";
    document.querySelector("#authSubmit").textContent = registering ? "Create account" : "Log in";
    document.querySelector("#authSwitch").textContent = registering
        ? "Already have an account? Log in"
        : "New here? Create an account";
    elements.authForm.elements.password.autocomplete = registering ? "new-password" : "current-password";
    if (!elements.authDialog.open) {
        elements.authDialog.showModal();
    }
}

async function submitAuth(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form));
    const path = state.authMode === "register" ? "/api/auth/register" : "/api/auth/login";
    const body = state.authMode === "register"
        ? {name: data.name, email: data.email, password: data.password, role: data.role}
        : {email: data.email, password: data.password};
    try {
        const auth = await request(path, {method: "POST", body: JSON.stringify(body)});
        setSession(auth);
        elements.authDialog.close();
        form.reset();
        showToast(`Welcome, ${auth.name}`);
    } catch (error) {
        showToast(error.message, true);
    }
}

async function submitBooking(event) {
    event.preventDefault();
    if (!state.token) {
        elements.listingDialog.close();
        openAuth("login");
        showToast("Log in before reserving a stay");
        return;
    }
    const data = Object.fromEntries(new FormData(event.currentTarget));
    try {
        const booking = await request("/api/bookings", {
            method: "POST",
            body: JSON.stringify({
                listingId: Number(data.listingId),
                checkIn: data.checkIn,
                checkOut: data.checkOut,
                guests: Number(data.guests)
            })
        });
        elements.listingDialog.close();
        showToast(`Reserved for ${money(booking.totalPrice)}`);
    } catch (error) {
        showToast(error.message, true);
    }
}

async function openTrips() {
    try {
        const bookings = await request("/api/bookings/mine");
        const list = document.querySelector("#tripList");
        list.innerHTML = bookings.length
            ? bookings.map(booking => `
                <article class="dashboard-item">
                    <img src="${escapeHtml(booking.listingImageUrl)}" alt="">
                    <div>
                        <h3>${escapeHtml(booking.listingTitle)}</h3>
                        <p>${shortDate(booking.checkIn)} – ${shortDate(booking.checkOut)} · ${money(booking.totalPrice)}</p>
                    </div>
                    <span class="status ${booking.status === "CANCELLED" ? "cancelled" : ""}">${booking.status}</span>
                </article>
            `).join("")
            : "<p>You have no trips yet.</p>";
        elements.tripsDialog.showModal();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function openHostDashboard() {
    try {
        const listings = await request("/api/listings/host/mine");
        renderHostListings(listings);
        elements.hostDialog.showModal();
    } catch (error) {
        showToast(error.message, true);
    }
}

function renderHostListings(listings) {
    document.querySelector("#hostListingList").innerHTML = listings.length
        ? listings.map(listing => `
            <article class="dashboard-item">
                <img src="${escapeHtml(listing.imageUrl)}" alt="">
                <div>
                    <h3>${escapeHtml(listing.title)}</h3>
                    <p>${escapeHtml(listing.city)} · ${money(listing.nightlyPrice)} per night</p>
                </div>
                <span class="status">${listing.reviewCount} reviews</span>
            </article>
        `).join("")
        : "<p>No active listings yet.</p>";
}

async function submitHostListing(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form));
    const body = {
        title: data.title,
        city: data.city,
        country: data.country,
        description: data.description,
        propertyType: data.propertyType,
        maxGuests: Number(data.maxGuests),
        bedrooms: Number(data.bedrooms),
        beds: Number(data.beds),
        bathrooms: Number(data.bathrooms),
        nightlyPrice: Number(data.nightlyPrice),
        imageUrl: data.imageUrl,
        amenities: data.amenities.split(",").map(value => value.trim()).filter(Boolean)
    };
    try {
        await request("/api/listings", {method: "POST", body: JSON.stringify(body)});
        form.reset();
        const listings = await request("/api/listings/host/mine");
        renderHostListings(listings);
        loadListings();
        showToast("Listing published");
    } catch (error) {
        showToast(error.message, true);
    }
}

function applySearch(event) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const params = new URLSearchParams();
    for (const [key, value] of data.entries()) {
        if (value) {
            params.set(key, value);
        }
    }
    loadListings(params);
    document.querySelector("#stays").scrollIntoView({behavior: "smooth"});
}

function clearSearch() {
    elements.searchForm.reset();
    elements.searchForm.elements.guests.value = 2;
    document.querySelectorAll(".filter-chip").forEach(chip =>
        chip.classList.toggle("active", chip.dataset.location === ""));
    loadListings();
}

function setDateMinimums() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dayAfter = new Date();
    dayAfter.setDate(dayAfter.getDate() + 2);
    const tomorrowText = tomorrow.toISOString().slice(0, 10);
    const dayAfterText = dayAfter.toISOString().slice(0, 10);
    document.querySelectorAll("input[name='checkIn']").forEach(input => {
        input.min = tomorrowText;
    });
    document.querySelectorAll("input[name='checkOut']").forEach(input => {
        input.min = dayAfterText;
    });
}

elements.searchForm.addEventListener("submit", applySearch);
elements.authForm.addEventListener("submit", submitAuth);
document.querySelector("#bookingForm").addEventListener("submit", submitBooking);
document.querySelector("#hostListingForm").addEventListener("submit", submitHostListing);
document.querySelector("#loginButton").addEventListener("click", () => openAuth("login"));
document.querySelector("#registerButton").addEventListener("click", () => openAuth("register"));
document.querySelector("#tripsButton").addEventListener("click", openTrips);
document.querySelector("#hostButton").addEventListener("click", openHostDashboard);
document.querySelector("#accountButton").addEventListener("click", () => {
    clearSession();
    showToast("You are logged out");
});
document.querySelector("#authSwitch").addEventListener("click", () =>
    openAuth(state.authMode === "login" ? "register" : "login"));
document.querySelector("#clearSearchButton").addEventListener("click", clearSearch);
elements.listingGrid.addEventListener("click", event => {
    const button = event.target.closest("[data-listing-id]");
    if (button) {
        openListing(button.dataset.listingId);
    }
});
document.querySelectorAll("[data-close]").forEach(button => {
    button.addEventListener("click", () => document.querySelector(`#${button.dataset.close}`).close());
});
document.querySelectorAll(".filter-chip").forEach(chip => {
    chip.addEventListener("click", () => {
        document.querySelectorAll(".filter-chip").forEach(item => item.classList.remove("active"));
        chip.classList.add("active");
        const params = new URLSearchParams();
        if (chip.dataset.location) {
            params.set("location", chip.dataset.location);
        }
        loadListings(params);
    });
});
document.querySelectorAll("[data-demo]").forEach(button => {
    button.addEventListener("click", () => {
        const host = button.dataset.demo === "host";
        elements.authForm.elements.email.value = host ? "host@nido.dev" : "guest@nido.dev";
        elements.authForm.elements.password.value = host ? "Host123!" : "Guest123!";
        elements.authForm.requestSubmit();
    });
});
document.querySelectorAll("dialog").forEach(dialog => {
    dialog.addEventListener("click", event => {
        const bounds = dialog.getBoundingClientRect();
        const inside = event.clientX >= bounds.left && event.clientX <= bounds.right
            && event.clientY >= bounds.top && event.clientY <= bounds.bottom;
        if (!inside) {
            dialog.close();
        }
    });
});

setDateMinimums();
updateNavigation();
loadListings();
