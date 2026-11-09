<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    String mode = request.getParameter("mode");
    if (mode == null || (!mode.equals("login") && !mode.equals("register"))) mode = "login";
    request.setAttribute("mode", mode);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:choose><c:when test="${mode == 'register'}">Create account</c:when><c:otherwise>Log in</c:otherwise></c:choose> | BAGORITHM</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/style.css" rel="stylesheet">
</head>
<body class="auth-page">
    <div class="auth-top"><a class="brand-mark" href="${pageContext.request.contextPath}/index.jsp">BAGORITHM<span>.</span></a><a href="${pageContext.request.contextPath}/index.jsp">← Back home</a></div>
    <main class="auth-card">
        <div class="auth-icon">✈</div>
        <c:choose>
            <c:when test="${mode == 'register'}">
                <p class="eyebrow">A better trip starts here</p><h1>Create your account</h1>
                <p class="muted-copy">Join BAGORITHM and make room for the good stuff.</p>
            </c:when>
            <c:otherwise>
                <p class="eyebrow">Welcome back</p><h1>Log in to BAGORITHM</h1>
                <p class="muted-copy">Your next adventure is waiting.</p>
            </c:otherwise>
        </c:choose>
        <c:if test="${not empty param.error}"><div class="alert alert-danger py-2"><c:out value="${param.error}"/></div></c:if>
        <c:if test="${not empty param.message}"><div class="alert alert-success py-2"><c:out value="${param.message}"/></div></c:if>
        <form method="post" action="${pageContext.request.contextPath}/app">
            <input type="hidden" name="action" value="${mode}">
            <c:if test="${mode == 'register'}">
                <label class="form-label" for="name">Name</label>
                <input class="form-control form-control-lg mb-3" id="name" name="name" maxlength="80" autocomplete="name" required>
            </c:if>
            <label class="form-label" for="email">Email address</label>
            <input class="form-control form-control-lg mb-3" id="email" name="email" type="email" maxlength="160" autocomplete="email" required>
            <label class="form-label" for="password">Password</label>
            <input class="form-control form-control-lg mb-3" id="password" name="password" type="password" minlength="8" autocomplete="${mode == 'register' ? 'new-password' : 'current-password'}" required>
            <c:if test="${mode == 'register'}">
                <label class="form-label" for="confirmPassword">Confirm password</label>
                <input class="form-control form-control-lg mb-3" id="confirmPassword" name="confirmPassword" type="password" minlength="8" autocomplete="new-password" required>
            </c:if>
            <button class="btn btn-coral btn-lg w-100 rounded-pill mt-2" type="submit"><c:choose><c:when test="${mode == 'register'}">Create account</c:when><c:otherwise>Log in</c:otherwise></c:choose></button>
        </form>
        <div class="auth-switch">
            <c:choose><c:when test="${mode == 'register'}">Already have an account? <a href="${pageContext.request.contextPath}/auth.jsp?mode=login">Log in</a></c:when>
                <c:otherwise>New to BAGORITHM? <a href="${pageContext.request.contextPath}/auth.jsp?mode=register">Create an account</a></c:otherwise></c:choose>
        </div>
    </main>
</body>
</html>
