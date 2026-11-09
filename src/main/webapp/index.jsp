<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>BAGORITHM | Pack smarter, travel farther</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/style.css" rel="stylesheet">
</head>
<body class="landing-page">
    <nav class="navbar navbar-expand-lg navbar-dark landing-nav">
        <div class="container">
            <a class="navbar-brand brand-mark" href="${pageContext.request.contextPath}/index.jsp">BAGORITHM<span>.</span></a>
            <div class="ms-auto d-flex gap-2">
                <a class="btn btn-link text-white text-decoration-none" href="${pageContext.request.contextPath}/auth.jsp?mode=login">Log in</a>
                <a class="btn btn-outline-light rounded-pill px-4" href="${pageContext.request.contextPath}/auth.jsp?mode=register">Create account</a>
            </div>
        </div>
    </nav>
    <main class="container hero-wrap">
        <div class="row align-items-center g-5">
            <div class="col-lg-6">
                <div class="eyebrow"><span class="eyebrow-dot"></span> Your journey, thoughtfully packed</div>
                <h1 class="hero-title">Go places.<br><span>Pack with purpose.</span></h1>
                <p class="hero-copy">A calmer way to prepare for every trip. Get a destination-aware packing list, keep plans in one place, and leave the last-minute scramble behind.</p>
                <a class="btn btn-coral btn-lg rounded-pill px-5 mt-3" href="${pageContext.request.contextPath}/auth.jsp?mode=register">Get started <span aria-hidden="true">→</span></a>
                <div class="hero-proof mt-4"><span>✦</span> Simple trip planning, made personal</div>
            </div>
            <div class="col-lg-6">
                <div class="travel-art" role="img" aria-label="Illustration of a suitcase ready for a trip">
                    <div class="sun"></div><div class="cloud cloud-one"></div><div class="cloud cloud-two"></div>
                    <div class="hill hill-back"></div><div class="hill hill-front"></div>
                    <div class="plane">✈</div>
                    <div class="suitcase">
                        <div class="case-handle"></div><div class="case-band"></div>
                        <div class="case-sticker">✦</div><div class="case-tag"></div>
                    </div>
                    <div class="art-caption"><span class="caption-icon">✓</span><span><b>Your next adventure</b><small>Everything in its place</small></span></div>
                </div>
            </div>
        </div>
        <div class="row g-3 feature-strip">
            <div class="col-md-4"><div class="feature-item"><span class="feature-icon">⌖</span><span><b>Made for your destination</b><small>Recommendations that fit your trip</small></span></div></div>
            <div class="col-md-4"><div class="feature-item"><span class="feature-icon">☷</span><span><b>One list, less stress</b><small>Track packing and luggage weight</small></span></div></div>
            <div class="col-md-4"><div class="feature-item"><span class="feature-icon">♡</span><span><b>Keep every detail close</b><small>Plans, reminders and memories</small></span></div></div>
        </div>
    </main>
    <footer class="landing-footer">BAGORITHM <span>·</span> Thoughtful travel starts here</footer>
</body>
</html>
