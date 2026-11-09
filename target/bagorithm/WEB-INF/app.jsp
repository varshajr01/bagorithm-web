<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>BAGORITHM | <c:out value="${section}"/></title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/style.css" rel="stylesheet">
</head>
<body class="app-page">
<aside class="sidebar">
    <a class="brand-mark sidebar-brand" href="${pageContext.request.contextPath}/app">BAGORITHM<span>.</span></a>
    <div class="sidebar-label">YOUR SPACE</div>
    <nav class="side-nav">
        <a class="${section == 'dashboard' ? 'active' : ''}" href="${pageContext.request.contextPath}/app">⌂ <span>Dashboard</span></a>
        <a class="${section == 'trips' ? 'active' : ''}" href="${pageContext.request.contextPath}/app?section=trips">✈ <span>My trips</span></a>
        <div class="sidebar-label side-label-trip">TRIP TOOLS</div>
        <a class="${section == 'packing' ? 'active' : ''}" href="${pageContext.request.contextPath}/app?section=packing&amp;tripId=${tripId}">☷ <span>Packing list</span></a>
        <a class="${section == 'itinerary' ? 'active' : ''}" href="${pageContext.request.contextPath}/app?section=itinerary&amp;tripId=${tripId}">⌖ <span>Itinerary</span></a>
        <a class="${section == 'shopping' ? 'active' : ''}" href="${pageContext.request.contextPath}/app?section=shopping&amp;tripId=${tripId}">＋ <span>Shopping list</span></a>
        <a class="${section == 'home' ? 'active' : ''}" href="${pageContext.request.contextPath}/app?section=home&amp;tripId=${tripId}">⌂ <span>Home checklist</span></a>
        <a class="${section == 'souvenirs' ? 'active' : ''}" href="${pageContext.request.contextPath}/app?section=souvenirs&amp;tripId=${tripId}">♡ <span>Souvenir ideas</span></a>
        <a class="${section == 'memories' ? 'active' : ''}" href="${pageContext.request.contextPath}/app?section=memories&amp;tripId=${tripId}">▧ <span>Travel memories</span></a>
    </nav>
    <div class="sidebar-bottom">
        <div class="user-chip"><span class="avatar"><c:out value="${userName.substring(0,1)}"/></span><span><b><c:out value="${userName}"/></b><small>Ready to explore</small></span></div>
        <form method="post" action="${pageContext.request.contextPath}/app"><input type="hidden" name="action" value="logout"><button class="logout-button" type="submit">↪ <span>Log out</span></button></form>
    </div>
</aside>
<main class="main-content">
    <header class="topbar">
        <div><span class="topbar-kicker">MAKE ROOM FOR ADVENTURE</span><div class="topbar-title">Plan your next great escape</div></div>
        <c:if test="${not empty trips}">
            <form method="get" action="${pageContext.request.contextPath}/app" class="trip-switcher">
                <input type="hidden" name="section" value="${section}">
                <label for="tripSelect">CURRENT TRIP</label>
                <select id="tripSelect" name="tripId" onchange="this.form.submit()">
                    <c:forEach var="optionTrip" items="${trips}">
                        <option value="${optionTrip.id}" ${optionTrip.id == tripId ? 'selected' : ''}><c:out value="${optionTrip.name}"/> · <c:out value="${optionTrip.destination}"/></option>
                    </c:forEach>
                </select>
            </form>
        </c:if>
    </header>
    <div class="content-inner">
        <c:if test="${not empty flash}"><div class="alert alert-success"><c:out value="${flash}"/></div></c:if>
        <c:if test="${not empty error}"><div class="alert alert-danger"><c:out value="${error}"/></div></c:if>

        <c:choose>
            <c:when test="${section == 'dashboard'}">
                <section class="welcome-banner">
                    <div><span class="banner-kicker">YOUR TRAVEL DASHBOARD</span><h1>Hi <c:out value="${userName}"/>, where to next?</h1>
                    <p>Every great journey begins with a little preparation.</p>
                    <a class="btn btn-coral rounded-pill px-4" href="${pageContext.request.contextPath}/app?section=trips#new-trip">Plan a new trip <span aria-hidden="true">→</span></a></div>
                    <div class="banner-symbol">✈</div>
                </section>
                <div class="row g-3 summary-grid">
                    <div class="col-sm-6 col-xl-4"><div class="summary-card"><span class="summary-icon coral-icon">✈</span><div class="summary-number"><c:out value="${tripCount}"/></div><div class="summary-label">Trips planned</div></div></div>
                    <div class="col-sm-6 col-xl-4"><div class="summary-card"><span class="summary-icon green-icon">✓</span><div class="summary-number"><c:out value="${packedCount}"/> <small>/ <c:out value="${packingCount}"/></small></div><div class="summary-label">Items packed for this trip</div></div></div>
                    <div class="col-sm-6 col-xl-4"><div class="summary-card"><span class="summary-icon blue-icon">⌖</span><div class="summary-number"><c:out value="${not empty trip ? trip.destination : '—'}"/></div><div class="summary-label">Selected destination</div></div></div>
                </div>
                <div class="section-heading"><div><span class="topbar-kicker">ON THE HORIZON</span><h2>Upcoming trips</h2></div><a class="text-link" href="${pageContext.request.contextPath}/app?section=trips">See all trips →</a></div>
                <c:choose><c:when test="${empty upcomingTrips}"><div class="empty-state"><div class="empty-icon">✈</div><h3>No upcoming trips yet</h3><p>Create a trip to get a personalized packing list and countdown.</p><a class="btn btn-coral rounded-pill px-4" href="${pageContext.request.contextPath}/app?section=trips#new-trip">Create your first trip</a></div></c:when>
                <c:otherwise><div class="row g-3"><c:forEach var="upcoming" items="${upcomingTrips}"><div class="col-lg-4"><a class="trip-card" href="${pageContext.request.contextPath}/app?section=packing&amp;tripId=${upcoming.id}"><div class="trip-card-top"><span class="trip-pill"><c:out value="${upcoming.purpose}"/></span><span class="trip-arrow">↗</span></div><h3><c:out value="${upcoming.name}"/></h3><p>⌖ <c:out value="${upcoming.destination}"/></p><div class="trip-card-date">◷ <c:out value="${upcoming.departure_date}"/> – <c:out value="${upcoming.return_date}"/></div></a></div></c:forEach></div></c:otherwise></c:choose>
            </c:when>

            <c:when test="${section == 'trips'}">
                <div class="section-heading page-heading"><div><span class="topbar-kicker">YOUR JOURNEYS</span><h1>My trips</h1><p>All your travel plans, together in one place.</p></div></div>
                <div id="new-trip" class="panel-card form-panel">
                    <div class="panel-title"><span class="panel-icon">＋</span><div><h2>Plan a new trip</h2><p>Tell us the basics and we’ll help with the packing.</p></div></div>
                    <form method="post" action="${pageContext.request.contextPath}/app" class="row g-3">
                        <input type="hidden" name="action" value="createTrip"><input type="hidden" name="section" value="trips">
                        <div class="col-md-6"><label class="form-label">Trip name</label><input class="form-control" name="name" maxlength="100" placeholder="e.g. Summer in Lisbon" required></div>
                        <div class="col-md-6"><label class="form-label">Destination</label><input class="form-control" name="destination" maxlength="120" placeholder="City, country or region" required></div>
                        <div class="col-md-6"><label class="form-label">Departure date</label><input class="form-control" type="date" name="departureDate" required></div>
                        <div class="col-md-6"><label class="form-label">Return date</label><input class="form-control" type="date" name="returnDate" required></div>
                        <div class="col-md-4"><label class="form-label">Travellers</label><input class="form-control" type="number" name="travellers" min="1" value="1" required></div>
                        <div class="col-md-4"><label class="form-label">Travel purpose</label><select class="form-select" name="purpose" required><option value="">Choose one</option><option>Leisure</option><option>Business</option><option>Adventure</option><option>Beach holiday</option><option>Family visit</option><option>Other</option></select></div>
                        <div class="col-md-4"><label class="form-label">Expected weather</label><select class="form-select" name="weather" required><option value="">Choose one</option><option>Warm and sunny</option><option>Hot and tropical</option><option>Cold / winter</option><option>Rainy</option><option>Mild</option></select></div>
                        <div class="col-md-4"><label class="form-label">Maximum luggage weight (kg)</label><input class="form-control" type="number" name="maxWeight" min="0.1" step="0.1" value="20" required></div>
                        <div class="col-12"><button class="btn btn-coral rounded-pill px-4" type="submit">Create trip &amp; packing list →</button></div>
                    </form>
                </div>
                <div class="section-heading mt-5"><div><span class="topbar-kicker">YOUR TRAVEL HISTORY</span><h2>Previous and upcoming trips</h2></div></div>
                <c:choose><c:when test="${empty records}"><div class="empty-state"><div class="empty-icon">⌖</div><h3>Your trip list is ready</h3><p>Trips you create will be saved here.</p></div></c:when>
                    <c:otherwise><div class="row g-3"><c:forEach var="tripRow" items="${records}"><div class="col-lg-6"><div class="trip-list-card"><div class="trip-list-icon">✈</div><div class="flex-grow-1"><h3><c:out value="${tripRow.name}"/></h3><p><c:out value="${tripRow.destination}"/> · <c:out value="${tripRow.purpose}"/></p><small><c:out value="${tripRow.departure_date}"/> — <c:out value="${tripRow.return_date}"/> · <c:out value="${tripRow.travellers}"/> traveller(s)</small></div><a class="btn btn-sm btn-soft rounded-pill" href="${pageContext.request.contextPath}/app?section=packing&amp;tripId=${tripRow.id}">Open trip</a></div></div></c:forEach></div></c:otherwise>
                </c:choose>
            </c:when>

            <c:otherwise>
                <c:choose>
                    <c:when test="${empty trip}"><div class="empty-state"><div class="empty-icon">✈</div><h3>Start with a trip</h3><p>Create a trip before adding plans, lists or memories.</p><a class="btn btn-coral rounded-pill px-4" href="${pageContext.request.contextPath}/app?section=trips#new-trip">Create a trip</a></div></c:when>
                    <c:otherwise>
                        <div class="section-heading page-heading"><div><span class="topbar-kicker"><c:out value="${trip.destination}"/> · <c:out value="${trip.name}"/></span>
                            <h1><c:choose><c:when test="${section == 'packing'}">Packing list</c:when><c:when test="${section == 'itinerary'}">Travel itinerary</c:when><c:when test="${section == 'shopping'}">Shopping list</c:when><c:when test="${section == 'home'}">Home preparation</c:when><c:when test="${section == 'souvenirs'}">Souvenir ideas</c:when><c:otherwise>Travel memories</c:otherwise></c:choose></h1>
                            <p><c:choose><c:when test="${daysRemaining > 0}"><c:out value="${daysRemaining}"/> days until departure · <c:out value="${trip.departure_date}"/></c:when><c:when test="${daysRemaining == 0}">Your trip starts today. Have a wonderful journey!</c:when><c:otherwise>This trip began <c:out value="${-daysRemaining}"/> days ago · <c:out value="${trip.departure_date}"/></c:otherwise></c:choose></p>
                        </div></div>

                        <c:if test="${section == 'packing'}">
                            <div class="row g-3 mb-4">
                                <div class="col-md-4"><div class="metric-card"><span class="metric-icon">✓</span><div><b><c:out value="${packedCount}"/> / <c:out value="${packingCount}"/></b><small>items packed</small></div></div></div>
                                <div class="col-md-4"><div class="metric-card"><span class="metric-icon">⚖</span><div><b><span id="total-weight"><fmt:formatNumber value="${totalWeight}" maxFractionDigits="2"/></span> kg</b><small>of <fmt:formatNumber value="${trip.max_weight}" maxFractionDigits="2"/> kg maximum</small></div></div></div>
                                <div class="col-md-4"><div class="metric-card ${remainingWeight < 0 ? 'metric-warning' : ''}" id="remaining-weight-card"><span class="metric-icon">◉</span><div><b><span id="remaining-weight" data-max-weight="${trip.max_weight}"><fmt:formatNumber value="${remainingWeight}" maxFractionDigits="2"/></span> kg</b><small id="remaining-weight-label"><c:choose><c:when test="${remainingWeight < 0}">Over the luggage limit</c:when><c:otherwise>weight remaining</c:otherwise></c:choose></small></div></div></div>
                            </div>
                            <div class="progress mb-4"><div class="progress-bar" role="progressbar" style="width: ${packingCount == 0 ? 0 : packedCount * 100 / packingCount}%"></div></div>
                            <div id="weight-warning" class="alert alert-warning ${remainingWeight >= 0 ? 'd-none' : ''}">Your packing list is <span id="over-limit-weight"><fmt:formatNumber value="${-remainingWeight}" maxFractionDigits="2"/></span> kg over the luggage limit. Update item weights or quantities.</div>
                            <div class="panel-card form-panel mb-4"><div class="panel-title"><span class="panel-icon">＋</span><div><h2>Add a packing item</h2><p>Recommendations are tailored to your destination, weather and purpose.</p></div></div>
                                <form method="post" action="${pageContext.request.contextPath}/app" class="row g-2 align-items-end">
                                    <input type="hidden" name="action" value="add"><input type="hidden" name="section" value="packing"><input type="hidden" name="tripId" value="${tripId}">
                                    <div class="col-md-3"><label class="form-label">Category</label><select class="form-select" name="category"><option>Clothing</option><option>Footwear</option><option>Toiletries</option><option>Electronics</option><option>Documents</option><option>Medicines</option><option>Travel essentials</option></select></div>
                                    <div class="col-md-4"><label class="form-label">Item</label><input class="form-control" name="name" maxlength="120" required></div>
                                    <div class="col-md-2"><label class="form-label">Quantity</label><input class="form-control" type="number" name="quantity" min="1" value="1" required></div>
                                    <div class="col-md-2"><label class="form-label">Each (kg)</label><input class="form-control" type="number" name="weight" min="0" step="0.01" value="0" required></div>
                                    <div class="col-md-1"><button class="btn btn-coral w-100" type="submit">+</button></div>
                                </form>
                            </div>
                            <c:choose><c:when test="${empty records}"><div class="empty-state"><h3>Your list is empty</h3><p>Add your first packing item above.</p></div></c:when>
                            <c:otherwise><div class="record-list"><c:forEach var="item" items="${records}"><div class="record-row">
                                <form method="post" action="${pageContext.request.contextPath}/app" class="toggle-form"><input type="hidden" name="action" value="toggle"><input type="hidden" name="section" value="packing"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${item.id}"><button class="check-button ${item.packed == 1 ? 'checked' : ''}" type="submit" aria-label="Toggle packed status"><c:if test="${item.packed == 1}">✓</c:if></button></form>
                                <form method="post" action="${pageContext.request.contextPath}/app" class="record-edit packing-edit"><input type="hidden" name="action" value="edit"><input type="hidden" name="section" value="packing"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${item.id}">
                                    <select class="form-select form-select-sm" name="category"><option ${item.category == 'Clothing' ? 'selected' : ''}>Clothing</option><option ${item.category == 'Footwear' ? 'selected' : ''}>Footwear</option><option ${item.category == 'Toiletries' ? 'selected' : ''}>Toiletries</option><option ${item.category == 'Electronics' ? 'selected' : ''}>Electronics</option><option ${item.category == 'Documents' ? 'selected' : ''}>Documents</option><option ${item.category == 'Medicines' ? 'selected' : ''}>Medicines</option><option ${item.category == 'Travel essentials' ? 'selected' : ''}>Travel essentials</option></select>
                                    <input class="form-control form-control-sm ${item.packed == 1 ? 'text-decoration-line-through' : ''}" name="name" value="<c:out value='${item.name}'/>" required>
                                    <input class="form-control form-control-sm quantity-input" type="number" min="1" name="quantity" value="${item.quantity}" required>
                                    <input class="form-control form-control-sm weight-input" type="number" min="0" step="0.01" name="weight" value="${item.weight}" required><span class="small-muted">kg each</span>
                                    <output class="small-muted item-total-weight"><fmt:formatNumber value="${item.total_weight}" maxFractionDigits="2"/> kg total</output>
                                    <button class="btn btn-sm btn-soft" type="submit">Save</button></form>
                                <form method="post" action="${pageContext.request.contextPath}/app"><input type="hidden" name="action" value="delete"><input type="hidden" name="section" value="packing"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${item.id}"><button class="delete-button" type="submit" aria-label="Delete item">×</button></form>
                            </div></c:forEach></div></c:otherwise></c:choose>
                        </c:if>

                        <c:if test="${section == 'itinerary'}">
                            <div class="panel-card form-panel mb-4"><div class="panel-title"><span class="panel-icon">＋</span><div><h2>Add a plan</h2><p>Keep your days organized, one stop at a time.</p></div></div>
                                <form method="post" action="${pageContext.request.contextPath}/app" class="row g-2 align-items-end"><input type="hidden" name="action" value="add"><input type="hidden" name="section" value="itinerary"><input type="hidden" name="tripId" value="${tripId}">
                                    <div class="col-md-2"><label class="form-label">Date</label><input class="form-control" type="date" name="date" required></div><div class="col-md-3"><label class="form-label">Activity</label><input class="form-control" name="activity" maxlength="160" required></div><div class="col-md-2"><label class="form-label">Time</label><input class="form-control" type="time" name="time" required></div><div class="col-md-3"><label class="form-label">Location</label><input class="form-control" name="location" maxlength="160" required></div><div class="col-md-2"><button class="btn btn-coral w-100" type="submit">Add to itinerary</button></div>
                                </form>
                            </div>
                            <c:choose><c:when test="${empty records}"><div class="empty-state"><h3>Your itinerary is waiting</h3><p>Add a travel plan above.</p></div></c:when><c:otherwise><div class="record-list"><c:forEach var="row" items="${records}"><div class="record-row">
                                <form method="post" action="${pageContext.request.contextPath}/app" class="record-edit"><input type="hidden" name="action" value="edit"><input type="hidden" name="section" value="itinerary"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${row.id}"><input class="form-control form-control-sm" type="date" name="date" value="${row.plan_date}" required><input class="form-control form-control-sm" name="activity" value="<c:out value='${row.activity}'/>" required><input class="form-control form-control-sm" type="time" name="time" value="${row.plan_time}" required><input class="form-control form-control-sm" name="location" value="<c:out value='${row.location}'/>" required><button class="btn btn-sm btn-soft" type="submit">Save</button></form>
                                <form method="post" action="${pageContext.request.contextPath}/app"><input type="hidden" name="action" value="delete"><input type="hidden" name="section" value="itinerary"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${row.id}"><button class="delete-button" type="submit">×</button></form>
                            </div></c:forEach></div></c:otherwise></c:choose>
                        </c:if>

                        <c:if test="${section == 'shopping'}">
                            <div class="panel-card form-panel mb-4"><div class="panel-title"><span class="panel-icon">＋</span><div><h2>Things to pick up</h2><p>Keep track of everything you need before you go.</p></div></div><form method="post" action="${pageContext.request.contextPath}/app" class="row g-2 align-items-end"><input type="hidden" name="action" value="add"><input type="hidden" name="section" value="shopping"><input type="hidden" name="tripId" value="${tripId}"><div class="col-md-8"><label class="form-label">Item</label><input class="form-control" name="name" required></div><div class="col-md-2"><label class="form-label">Quantity</label><input class="form-control" type="number" min="1" name="quantity" value="1" required></div><div class="col-md-2"><button class="btn btn-coral w-100" type="submit">Add item</button></div></form></div>
                            <c:choose><c:when test="${empty records}"><div class="empty-state"><h3>Nothing to shop for yet</h3><p>Add items you want to buy before travelling.</p></div></c:when><c:otherwise><div class="record-list"><c:forEach var="row" items="${records}"><div class="record-row"><form method="post" action="${pageContext.request.contextPath}/app" class="toggle-form"><input type="hidden" name="action" value="toggle"><input type="hidden" name="section" value="shopping"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${row.id}"><button class="check-button ${row.purchased == 1 ? 'checked' : ''}" type="submit"><c:if test="${row.purchased == 1}">✓</c:if></button></form><form method="post" action="${pageContext.request.contextPath}/app" class="record-edit"><input type="hidden" name="action" value="edit"><input type="hidden" name="section" value="shopping"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${row.id}"><input class="form-control form-control-sm" name="name" value="<c:out value='${row.name}'/>" required><input class="form-control form-control-sm quantity-input" type="number" min="1" name="quantity" value="${row.quantity}" required><button class="btn btn-sm btn-soft" type="submit">Save</button></form><form method="post" action="${pageContext.request.contextPath}/app"><input type="hidden" name="action" value="delete"><input type="hidden" name="section" value="shopping"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${row.id}"><button class="delete-button" type="submit">×</button></form></div></c:forEach></div></c:otherwise></c:choose>
                        </c:if>

                        <c:if test="${section == 'home'}">
                            <div class="panel-card form-panel mb-4"><div class="panel-title"><span class="panel-icon">＋</span><div><h2>Prepare your home</h2><p>A few small checks make coming home easier.</p></div></div><form method="post" action="${pageContext.request.contextPath}/app" class="row g-2"><input type="hidden" name="action" value="add"><input type="hidden" name="section" value="home"><input type="hidden" name="tripId" value="${tripId}"><div class="col-md-9"><input class="form-control" name="task" placeholder="Add another task" required></div><div class="col-md-3"><button class="btn btn-coral w-100" type="submit">Add task</button></div></form></div>
                            <div class="record-list"><c:forEach var="row" items="${records}"><div class="record-row"><form method="post" action="${pageContext.request.contextPath}/app" class="toggle-form"><input type="hidden" name="action" value="toggle"><input type="hidden" name="section" value="home"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${row.id}"><button class="check-button ${row.completed == 1 ? 'checked' : ''}" type="submit"><c:if test="${row.completed == 1}">✓</c:if></button></form><form method="post" action="${pageContext.request.contextPath}/app" class="record-edit"><input type="hidden" name="action" value="edit"><input type="hidden" name="section" value="home"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${row.id}"><input class="form-control form-control-sm" name="task" value="<c:out value='${row.task}'/>" required><button class="btn btn-sm btn-soft" type="submit">Save</button></form><form method="post" action="${pageContext.request.contextPath}/app"><input type="hidden" name="action" value="delete"><input type="hidden" name="section" value="home"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${row.id}"><button class="delete-button" type="submit">×</button></form></div></c:forEach></div>
                        </c:if>

                        <c:if test="${section == 'souvenirs'}">
                            <div class="panel-card form-panel mb-4"><div class="panel-title"><span class="panel-icon">＋</span><div><h2>Save an idea</h2><p>Keep a note of something special to bring home.</p></div></div><form method="post" action="${pageContext.request.contextPath}/app" class="row g-2 align-items-end"><input type="hidden" name="action" value="add"><input type="hidden" name="section" value="souvenirs"><input type="hidden" name="tripId" value="${tripId}"><div class="col-md-3"><label class="form-label">Item</label><input class="form-control" name="name" required></div><div class="col-md-3"><label class="form-label">Destination</label><input class="form-control" name="destination" value="<c:out value='${trip.destination}'/>" required></div><div class="col-md-4"><label class="form-label">Notes</label><input class="form-control" name="notes"></div><div class="col-md-2"><button class="btn btn-coral w-100" type="submit">Save idea</button></div></form></div>
                            <c:choose><c:when test="${empty records}"><div class="empty-state"><h3>Save a little inspiration</h3><p>Record the souvenirs you hope to find.</p></div></c:when><c:otherwise><div class="record-list"><c:forEach var="row" items="${records}"><div class="record-row"><span class="souvenir-icon">♡</span><form method="post" action="${pageContext.request.contextPath}/app" class="record-edit"><input type="hidden" name="action" value="edit"><input type="hidden" name="section" value="souvenirs"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${row.id}"><input class="form-control form-control-sm" name="name" value="<c:out value='${row.name}'/>" required><input class="form-control form-control-sm" name="destination" value="<c:out value='${row.destination}'/>" required><input class="form-control form-control-sm" name="notes" value="<c:out value='${row.notes}'/>"><button class="btn btn-sm btn-soft" type="submit">Save</button></form><form method="post" action="${pageContext.request.contextPath}/app"><input type="hidden" name="action" value="delete"><input type="hidden" name="section" value="souvenirs"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${row.id}"><button class="delete-button" type="submit">×</button></form></div></c:forEach></div></c:otherwise></c:choose>
                        </c:if>

                        <c:if test="${section == 'memories'}">
                            <div class="panel-card form-panel mb-4"><div class="panel-title"><span class="panel-icon">＋</span><div><h2>Add a travel photo</h2><p>JPG, PNG, GIF or WEBP · maximum 5 MB.</p></div></div><form method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/app" class="row g-2 align-items-end"><input type="hidden" name="action" value="upload"><input type="hidden" name="section" value="memories"><input type="hidden" name="tripId" value="${tripId}"><div class="col-md-5"><label class="form-label">Photo</label><input class="form-control" type="file" name="photo" accept="image/jpeg,image/png,image/gif,image/webp" required></div><div class="col-md-5"><label class="form-label">Caption</label><input class="form-control" name="caption" maxlength="160" placeholder="A moment worth remembering"></div><div class="col-md-2"><button class="btn btn-coral w-100" type="submit">Upload photo</button></div></form></div>
                            <c:choose><c:when test="${empty records}"><div class="empty-state"><div class="empty-icon">▧</div><h3>Your memories will live here</h3><p>Upload a photo from this trip to start your gallery.</p></div></c:when><c:otherwise><div class="row g-3 memory-grid"><c:forEach var="row" items="${records}"><div class="col-sm-6 col-lg-4"><div class="memory-card"><img src="${pageContext.request.contextPath}/app?photo=${row.id}" alt="<c:out value='${row.caption}'/>"><div class="memory-caption"><span><c:out value="${row.caption}"/></span><form method="post" action="${pageContext.request.contextPath}/app"><input type="hidden" name="action" value="delete"><input type="hidden" name="section" value="memories"><input type="hidden" name="tripId" value="${tripId}"><input type="hidden" name="id" value="${row.id}"><button class="delete-button" type="submit" aria-label="Delete photo">×</button></form></div></div></div></c:forEach></div></c:otherwise></c:choose>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </c:otherwise>
        </c:choose>
        <footer class="app-footer">Made for the journeys that stay with you. <span>✦</span></footer>
    </div>
</main>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<c:if test="${section == 'packing'}">
<script>
    (() => {
        const totalElement = document.getElementById("total-weight");
        const remainingElement = document.getElementById("remaining-weight");
        const remainingCard = document.getElementById("remaining-weight-card");
        const remainingLabel = document.getElementById("remaining-weight-label");
        const warning = document.getElementById("weight-warning");
        const overLimitElement = document.getElementById("over-limit-weight");
        if (!totalElement || !remainingElement) {
            return;
        }
        const formatter = new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 });
        const maxWeight = Number(remainingElement.dataset.maxWeight);

        const recalculateWeight = () => {
            let totalWeight = 0;
            let hasInvalidItem = false;
            document.querySelectorAll(".packing-edit").forEach((form) => {
                const quantityInput = form.querySelector('[name="quantity"]');
                const weightInput = form.querySelector('[name="weight"]');
                const itemTotal = form.querySelector(".item-total-weight");
                const quantity = Number(quantityInput.value);
                const unitWeight = Number(weightInput.value);
                if (!quantityInput.value || !weightInput.value || !Number.isSafeInteger(quantity)
                        || quantity < 1 || !Number.isFinite(unitWeight) || unitWeight < 0) {
                    itemTotal.textContent = "— kg total";
                    hasInvalidItem = true;
                    return;
                }
                const itemWeight = unitWeight * quantity;
                itemTotal.textContent = formatter.format(itemWeight) + " kg total";
                totalWeight += itemWeight;
            });

            if (hasInvalidItem || !Number.isFinite(totalWeight) || !Number.isFinite(maxWeight)) {
                totalElement.textContent = "—";
                remainingElement.textContent = "—";
                remainingLabel.textContent = "Update item values";
                remainingCard.classList.remove("metric-warning");
                warning.classList.add("d-none");
                return;
            }

            const remainingWeight = maxWeight - totalWeight;
            totalElement.textContent = formatter.format(totalWeight);
            remainingElement.textContent = formatter.format(remainingWeight);
            remainingLabel.textContent = remainingWeight < 0
                    ? "Over the luggage limit" : "weight remaining";
            remainingCard.classList.toggle("metric-warning", remainingWeight < 0);
            warning.classList.toggle("d-none", remainingWeight >= 0);
            overLimitElement.textContent = formatter.format(Math.abs(remainingWeight));
        };

        document.querySelectorAll(".packing-edit [name='quantity'], .packing-edit [name='weight']")
                .forEach((input) => input.addEventListener("input", recalculateWeight));
    })();
</script>
</c:if>
</body>
</html>
