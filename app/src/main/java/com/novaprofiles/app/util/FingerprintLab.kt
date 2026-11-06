package com.novaprofiles.app.util

import org.json.JSONObject

/**
 * Fingerprint Lab (v2.0 Pro): runs a live fingerprint check for a profile
 * inside a WebView and reports what a site would actually see.
 *
 * The lab JS runs AFTER the profile's FingerprintJs injection, so the
 * values it reads are the spoofed ones. The screen compares them with the
 * device's real values (real UA, real screen, real timezone, real locale)
 * and marks each row "Spoofed" (detected != real) or "Exposed"
 * (detected == real). Canvas is read twice: different hashes mean the
 * noise injection is working.
 *
 * Results arrive via the NovaLab JavascriptInterface (the WebRTC ICE
 * gather is async, so a plain evaluateJavascript return cannot carry it).
 */
object FingerprintLab {

    data class LabResult(
        val userAgent: String = "",
        val language: String = "",
        val timezone: String = "",
        val screen: String = "",
        val deviceMemory: String = "",
        val hardwareConcurrency: String = "",
        val platform: String = "",
        val doNotTrack: String = "",
        val canvasHash1: String = "",
        val canvasHash2: String = "",
        val webglVendor: String = "",
        val webglRenderer: String = "",
        val webrtcLeak: String = "", // "none" or the leaked local IP
        val touchPoints: String = ""
    ) {
        val canvasNoisy: Boolean get() = canvasHash1.isNotBlank() && canvasHash1 != canvasHash2
    }

    /** JS: gathers everything, then posts JSON to window.NovaLab.postResult. */
    fun script(): String = """
        (function() {
          function hash(s) {
            var h = 5381;
            for (var i = 0; i < s.length; i++) { h = ((h << 5) + h + s.charCodeAt(i)) | 0; }
            return (h >>> 0).toString(16);
          }
          function canvasHash() {
            try {
              var c = document.createElement('canvas');
              c.width = 200; c.height = 50;
              var x = c.getContext('2d');
              x.fillStyle = '#f60'; x.fillRect(0, 0, 200, 50);
              x.fillStyle = '#069'; x.font = '16px Arial';
              x.fillText('NovaProfile lab 123', 4, 30);
              x.fillStyle = 'rgba(0,0,0,0.3)';
              x.fillRect(10, 10, 80, 20);
              return hash(c.toDataURL());
            } catch (e) { return 'error'; }
          }
          function webglInfo() {
            try {
              var c = document.createElement('canvas');
              var gl = c.getContext('webgl') || c.getContext('experimental-webgl');
              if (!gl) return { vendor: 'none', renderer: 'none' };
              var ext = gl.getExtension('WEBGL_debug_renderer_info');
              if (!ext) return { vendor: 'hidden', renderer: 'hidden' };
              return {
                vendor: gl.getParameter(ext.UNMASKED_VENDOR_WEBGL) || '',
                renderer: gl.getParameter(ext.UNMASKED_RENDERER_WEBGL) || ''
              };
            } catch (e) { return { vendor: 'error', renderer: 'error' }; }
          }
          function iceLeak() {
            return new Promise(function(resolve) {
              var leak = 'none';
              try {
                var pc = new (window.RTCPeerConnection || window.webkitRTCPeerConnection)({
                  iceServers: []
                });
                pc.createDataChannel('lab');
                pc.onicecandidate = function(e) {
                  if (e.candidate) {
                    var m = e.candidate.candidate.match(
                      /(192\.168\.\d+\.\d+|10\.\d+\.\d+\.\d+|172\.(1[6-9]|2\d|3[0-1])\.\d+\.\d+)/);
                    if (m) leak = m[0];
                  }
                };
                pc.createOffer().then(function(o) { return pc.setLocalDescription(o); })
                  .catch(function() {});
                setTimeout(function() { try { pc.close(); } catch (e) {} resolve(leak); }, 2000);
              } catch (e) { resolve('blocked'); }
            });
          }
          var gl = webglInfo();
          var base = {
            userAgent: navigator.userAgent || '',
            language: navigator.language || '',
            timezone: (Intl.DateTimeFormat().resolvedOptions().timeZone) || '',
            screen: screen.width + 'x' + screen.height + 'x' + screen.colorDepth,
            deviceMemory: String(navigator.deviceMemory || ''),
            hardwareConcurrency: String(navigator.hardwareConcurrency || ''),
            platform: navigator.platform || '',
            doNotTrack: navigator.doNotTrack || '',
            canvasHash1: canvasHash(),
            canvasHash2: canvasHash(),
            webglVendor: gl.vendor,
            webglRenderer: gl.renderer,
            touchPoints: String(navigator.maxTouchPoints || 0)
          };
          iceLeak().then(function(leak) {
            base.webrtcLeak = leak;
            try { window.NovaLab.postResult(JSON.stringify(base)); }
            catch (e) { window.NovaLab.postResult('{"error":"' + e.message + '"}'); }
          });
        })();
    """.trimIndent()

    fun parse(json: String): LabResult {
        return try {
            val o = JSONObject(json)
            LabResult(
                userAgent = o.optString("userAgent"),
                language = o.optString("language"),
                timezone = o.optString("timezone"),
                screen = o.optString("screen"),
                deviceMemory = o.optString("deviceMemory"),
                hardwareConcurrency = o.optString("hardwareConcurrency"),
                platform = o.optString("platform"),
                doNotTrack = o.optString("doNotTrack"),
                canvasHash1 = o.optString("canvasHash1"),
                canvasHash2 = o.optString("canvasHash2"),
                webglVendor = o.optString("webglVendor"),
                webglRenderer = o.optString("webglRenderer"),
                webrtcLeak = o.optString("webrtcLeak"),
                touchPoints = o.optString("touchPoints")
            )
        } catch (e: Exception) {
            LabResult()
        }
    }
}
