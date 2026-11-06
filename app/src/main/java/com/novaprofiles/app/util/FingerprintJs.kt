package com.novaprofiles.app.util

import com.novaprofiles.app.data.Profile

/**
 * JS-level fingerprint overrides, injected into every page load for the
 * running profile (see BrowserActivity).
 *
 * This is an honest, limited technique: it overrides the values a page
 * reads through standard JS APIs (Canvas, WebGL, navigator, screen,
 * AudioContext, WebRTC). It does NOT patch the rendering engine, so
 * advanced fingerprint tests (timing, font enumeration, TLS, HTTP
 * headers) can still detect the real environment. The README says so.
 *
 * All values come from the user's own profile — nothing is copied from
 * any other product.
 */
object FingerprintJs {

    private fun jsString(value: String): String =
        "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"

    fun build(profile: Profile): String {
        val sb = StringBuilder()
        sb.append("(function(){\n")
        sb.append("try{\n")

        // --- navigator overrides ---
        sb.append("var nav = window.navigator;\n")
        sb.append("function def(obj, prop, val){ try{ Object.defineProperty(obj, prop, { get: function(){ return val; }, configurable: true }); }catch(e){} }\n")
        sb.append("def(nav, 'hardwareConcurrency', ${profile.hardwareConcurrency});\n")
        sb.append("def(nav, 'deviceMemory', ${profile.deviceMemory});\n")
        sb.append("def(nav, 'languages', [${jsString(profile.language)}]);\n")
        sb.append("def(nav, 'language', ${jsString(profile.language)});\n")
        if (profile.doNotTrack) {
            sb.append("def(nav, 'doNotTrack', '1');\n")
        }
        val ua = if (profile.userAgent.isNotBlank()) profile.userAgent
        else Profile.defaultUserAgent(profile.userAgentPreset)
        sb.append("def(nav, 'userAgent', ${jsString(ua)});\n")
        // v2.6: platform follows the profile OS (Android/Windows/macOS/Linux).
        sb.append("def(nav, 'platform', ${jsString(Profile.osPlatform(profile.os))});\n")

        // --- screen overrides ---
        sb.append("def(window.screen, 'width', ${profile.screenWidth});\n")
        sb.append("def(window.screen, 'height', ${profile.screenHeight});\n")
        sb.append("def(window.screen, 'availWidth', ${profile.screenWidth});\n")
        sb.append("def(window.screen, 'availHeight', ${profile.screenHeight});\n")

        // --- Canvas: "real" = no override; "noise" = stable per-profile
        // noise; "block" (v2.6) = reads return blank, like GoLogin's Block.
        if (profile.canvasMode == "block") {
            sb.append(
                """
                HTMLCanvasElement.prototype.toDataURL = function(){ return 'data:,'; };
                CanvasRenderingContext2D.prototype.getImageData = function(x, y, w, h){ return this.createImageData(w, h); };
                """.trimIndent()
            )
        }
        // --- Canvas noise (stable per profile id so the same profile
        // reads consistently across pages, but differently per profile) ---
        if (profile.canvasMode == "noise") {
            sb.append(
                """
                var seed = ${profile.id + 7919};
                function rnd(){ seed = (seed * 1103515245 + 12345) & 0x7fffffff; return seed / 0x7fffffff; }
                var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
                HTMLCanvasElement.prototype.toDataURL = function(){
                    var ctx = this.getContext('2d');
                    if (ctx) { try { var img = ctx.getImageData(0, 0, this.width, this.height); for (var i = 0; i < img.data.length; i += 4) { img.data[i] = (img.data[i] + Math.floor(rnd() * 3)) % 256; } ctx.putImageData(img, 0, 0); } catch (e) {} }
                    return origToDataURL.apply(this, arguments);
                };
                var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;
                CanvasRenderingContext2D.prototype.getImageData = function(x, y, w, h){
                    var d = origGetImageData.apply(this, arguments);
                    for (var i = 0; i < d.data.length; i += 4) { d.data[i] = (d.data[i] + Math.floor(rnd() * 3)) % 256; }
                    return d;
                };
                """.trimIndent()
            )
        }

        // --- WebGL vendor / renderer (v2.6: only when MASKED; REAL
        // reports the device's own GPU, like GoLogin's Real) ---
        if (profile.webglMode == "MASKED") {
            sb.append(
                """
                var origGetParameter = WebGLRenderingContext.prototype.getParameter;
                WebGLRenderingContext.prototype.getParameter = function(p){
                    if (p === 0x9245) return ${jsString(profile.webglVendor)};
                    if (p === 0x9246) return ${jsString(profile.webglRenderer)};
                    return origGetParameter.apply(this, arguments);
                };
                var origGetParameter2 = WebGL2RenderingContext.prototype.getParameter;
                WebGL2RenderingContext.prototype.getParameter = function(p){
                    if (p === 0x9245) return ${jsString(profile.webglVendor)};
                    if (p === 0x9246) return ${jsString(profile.webglRenderer)};
                    return origGetParameter2.apply(this, arguments);
                };
                """.trimIndent()
            )
        }

        // --- AudioContext (v2.6: only when MASKED; REAL leaves it alone) ---
        if (profile.audioMode == "MASKED") {
            sb.append(
                """
                try {
                    var origGetChannelData = AudioBuffer.prototype.getChannelData;
                    AudioBuffer.prototype.getChannelData = function(){
                        var d = origGetChannelData.apply(this, arguments);
                        for (var i = 0; i < d.length; i += 97) { d[i] = d[i] + 0.000001; }
                        return d;
                    };
                } catch (e) {}
                """.trimIndent()
            )
        }

        // --- Fonts (v2.6): Masked reports only the profile's font list
        // through document.fonts.check — the API leak-check sites use.
        // Real leaves the check alone. fontsMasking was a dead field
        // before v2.6 (stored, never applied); now it is wired.
        if (profile.fontsMasking) {
            val fontsJson = profile.fontsList.split(",")
                .map { it.trim() }.filter { it.isNotEmpty() }
                .joinToString(", ") { jsString(it) }
            sb.append(
                """
                try {
                    var maskedFonts = [${fontsJson}];
                    var genericFamilies = ['serif', 'sans-serif', 'monospace', 'cursive', 'fantasy', 'system-ui'];
                    document.fonts.check = function(font, text){
                        try {
                            var f = String(font).toLowerCase();
                            for (var i = 0; i < maskedFonts.length; i++) {
                                if (f.indexOf(maskedFonts[i].toLowerCase()) !== -1) return true;
                            }
                            for (var j = 0; j < genericFamilies.length; j++) {
                                if (f.indexOf(genericFamilies[j]) !== -1) return true;
                            }
                            return false;
                        } catch (e) { return false; }
                    };
                } catch (e) {}
                """.trimIndent()
            )
        }

        // --- Media devices (v2.6): Masked reports 0/0/0 devices, like
        // GoLogin's "Mask media devices". Real leaves enumeration alone.
        if (profile.mediaMasked) {
            sb.append(
                """
                try {
                    if (nav.mediaDevices && nav.mediaDevices.enumerateDevices) {
                        nav.mediaDevices.enumerateDevices = function(){ return Promise.resolve([]); };
                    }
                } catch (e) {}
                """.trimIndent()
            )
        }

        // --- Geolocation (v2.6): "IP" returns the proxy IP's coordinates
        // (profile latitude/longitude, set from ip-api at assign/run);
        // "OFF" denies, like a device with location switched off.
        if (profile.geoMode == "IP" && profile.latitude != null && profile.longitude != null) {
            val lat = profile.latitude
            val lon = profile.longitude
            sb.append(
                """
                try {
                    var geoPos = { coords: { latitude: $lat, longitude: $lon, accuracy: ${profile.geoAccuracy}, altitude: null, altitudeAccuracy: null, heading: null, speed: null }, timestamp: Date.now() };
                    if (nav.geolocation) {
                        nav.geolocation.getCurrentPosition = function(success, error, options){ try { success(geoPos); } catch (e) {} };
                        nav.geolocation.watchPosition = function(success, error, options){ try { success(geoPos); } catch (e) {} return 1; };
                        nav.geolocation.clearWatch = function(id){};
                    }
                } catch (e) {}
                """.trimIndent()
            )
        } else if (profile.geoMode == "OFF") {
            sb.append(
                """
                try {
                    if (nav.geolocation) {
                        nav.geolocation.getCurrentPosition = function(success, error, options){ try { error({ code: 1, message: 'User denied Geolocation', PERMISSION_DENIED: 1 }); } catch (e) {} };
                        nav.geolocation.watchPosition = function(success, error, options){ try { error({ code: 1, message: 'User denied Geolocation', PERMISSION_DENIED: 1 }); } catch (e) {} return 1; };
                        nav.geolocation.clearWatch = function(id){};
                    }
                } catch (e) {}
                """.trimIndent()
            )
        }

        // --- WebRTC ---
        if (profile.webrtcMode == "disable") {
            sb.append(
                """
                try { window.RTCPeerConnection = function(){ throw new Error('WebRTC is disabled for this profile'); }; } catch (e) {}
                try { window.webkitRTCPeerConnection = window.RTCPeerConnection; } catch (e) {}
                """.trimIndent()
            )
        }

        // --- Timezone: pages read it via Intl; override the resolved options ---
        if (profile.timezone != "Auto") {
            sb.append(
                """
                try {
                    var origResolved = Intl.DateTimeFormat.prototype.resolvedOptions;
                    Intl.DateTimeFormat.prototype.resolvedOptions = function(){
                        var o = origResolved.apply(this, arguments);
                        o.timeZone = ${jsString(profile.timezone)};
                        return o;
                    };
                } catch (e) {}
                """.trimIndent()
            )
        }

        sb.append("}catch(e){}\n")
        sb.append("})();")
        return sb.toString()
    }
}
