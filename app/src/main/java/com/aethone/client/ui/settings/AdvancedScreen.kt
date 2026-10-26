package com.aethone.client.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.aethone.client.R
import com.aethone.client.data.MasqueCarrier
import com.aethone.client.data.StackMode
import com.aethone.client.ui.AppViewModel

/** Row that opens a text input dialog and saves into the settings. */
@Composable
private fun TextInputRow(
    title: String,
    value: String,
    desc: String? = null,
    placeholder: String = "",
    multiline: Boolean = false,
    onSave: (String) -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    SettingRow(
        title = title,
        desc = value.ifBlank { desc ?: "" },
        onClick = { show = true },
        trailing = { ValueText("›") }
    )
    if (show) {
        InputDialog(
            title = title,
            initial = value,
            hint = placeholder,
            multiline = multiline,
            onDismiss = { show = false },
            onConfirm = { v -> onSave(v); show = false },
        )
    }
}

/** Row that opens a numeric input dialog. */
@Composable
private fun NumberInputRow(
    title: String,
    value: Int,
    desc: String? = null,
    onSave: (Int) -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    SettingRow(
        title = title,
        desc = value.toString(),
        onClick = { show = true },
        trailing = { ValueText("›") }
    )
    if (show) {
        InputDialog(
            title = title,
            initial = value.toString(),
            numeric = true,
            onDismiss = { show = false },
            onConfirm = { v -> onSave(v.toIntOrNull() ?: value); show = false },
        )
    }
}

/**
 * Every remaining core option, grouped and phrased for humans.
 * Text values open an input dialog; enums open a radio sheet.
 */
@Composable
fun AdvancedScreen(vm: AppViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()

    var showCarrier by remember { mutableStateOf(false) }
    var showStack by remember { mutableStateOf(false) }

    SettingsScaffold(stringResource(R.string.advanced_title), onBack) {

        // ------------------------------------------------------ endpoint
        SettingsSection(stringResource(R.string.advanced_endpoint)) {
            TextInputRow(stringResource(R.string.advanced_peer), s.peer, stringResource(R.string.advanced_peer_desc), "162.159.196.1:443") { v -> vm.update { it.copy(peer = v) } }
            TextInputRow(stringResource(R.string.advanced_gool_peer), s.goolPeer, placeholder = "162.159.192.1:2408") { v -> vm.update { it.copy(goolPeer = v) } }
            TextInputRow(stringResource(R.string.advanced_wiw_outer), s.wiwOuter, placeholder = "162.159.192.1:2408") { v -> vm.update { it.copy(wiwOuter = v) } }
            TextInputRow(stringResource(R.string.advanced_wiw_inner), s.wiwInner, placeholder = "188.114.96.1:2408") { v -> vm.update { it.copy(wiwInner = v) } }
            TextInputRow(stringResource(R.string.advanced_mim_outer), s.mimOuter, placeholder = "162.159.192.1:443") { v -> vm.update { it.copy(mimOuter = v) } }
            TextInputRow(stringResource(R.string.advanced_mim_inner), s.mimInner, placeholder = "162.159.204.1:443") { v -> vm.update { it.copy(mimInner = v) } }
        }

        // ------------------------------------------------------ carrier
        SettingsSection(stringResource(R.string.advanced_carrier)) {
            SettingRow(
                title = stringResource(R.string.advanced_carrier),
                desc = if (s.carrier == MasqueCarrier.H2) stringResource(R.string.advanced_carrier_h2)
                else stringResource(R.string.advanced_carrier_h3),
                onClick = { showCarrier = true },
                trailing = { ValueText("›") }
            )
            SwitchRow(
                title = stringResource(R.string.advanced_quic_v2),
                desc = stringResource(R.string.advanced_quic_v2_desc),
                checked = s.quicV2,
                onChange = { v -> vm.update { it.copy(quicV2 = v) } },
            )
            SwitchRow(
                title = stringResource(R.string.advanced_fragment),
                desc = stringResource(R.string.advanced_fragment_desc),
                checked = s.fragment,
                onChange = { v -> vm.update { it.copy(fragment = v) } },
            )
            if (s.fragment) {
                NumberInputRow(stringResource(R.string.advanced_fragment_size), s.fragmentSize) { v ->
                    vm.update { it.copy(fragmentSize = v) }
                }
                NumberInputRow(stringResource(R.string.advanced_fragment_delay), s.fragmentDelay) { v ->
                    vm.update { it.copy(fragmentDelay = v) }
                }
            }
        }

        // ------------------------------------------------------ DNS
        SettingsSection(stringResource(R.string.advanced_dns_section)) {
            SwitchRow(
                title = stringResource(R.string.advanced_dns_fake_ip),
                desc = stringResource(R.string.advanced_dns_fake_ip_desc),
                checked = s.dnsFakeIp,
                onChange = { v -> vm.update { it.copy(dnsFakeIp = v) } },
            )
            SwitchRow(
                title = stringResource(R.string.advanced_dns_hijack),
                desc = stringResource(R.string.advanced_dns_hijack_desc),
                checked = s.dnsHijack,
                onChange = { v -> vm.update { it.copy(dnsHijack = v) } },
            )
        }

        // ------------------------------------------------------ network
        SettingsSection(stringResource(R.string.advanced_network_section)) {
            SwitchRow(
                title = stringResource(R.string.advanced_ipv6),
                checked = s.ipv6,
                onChange = { v -> vm.update { it.copy(ipv6 = v) } },
            )
            NumberInputRow(stringResource(R.string.advanced_mtu), s.mtu, stringResource(R.string.advanced_mtu_desc)) { v ->
                vm.update { it.copy(mtu = v.coerceIn(1280, 65535)) }
            }
            SettingRow(
                title = stringResource(R.string.advanced_stack),
                desc = if (s.stackMode == StackMode.HYBRID) stringResource(R.string.advanced_stack_hybrid)
                else stringResource(R.string.advanced_stack_userspace),
                onClick = { showStack = true },
                trailing = { ValueText("›") }
            )
            TextInputRow(stringResource(R.string.advanced_upstream), s.upstream, stringResource(R.string.advanced_upstream_desc), "socks5://127.0.0.1:1080") { v ->
                vm.update { it.copy(upstream = v) }
            }
        }

        // ------------------------------------------------------ TLS / ECH
        SettingsSection(stringResource(R.string.advanced_tls_section)) {
            SwitchRow(
                title = stringResource(R.string.advanced_ech),
                desc = stringResource(R.string.advanced_ech_desc),
                checked = s.ech,
                onChange = { v -> vm.update { it.copy(ech = v) } },
            )
            TextInputRow(stringResource(R.string.advanced_tls_ciphers), s.tlsCiphers, placeholder = "AES128-GCM-SHA256:...") { v -> vm.update { it.copy(tlsCiphers = v) } }
            TextInputRow(stringResource(R.string.advanced_tls13_ciphers), s.tls13Ciphers, placeholder = "TLS_AES_128_GCM_SHA256:...") { v -> vm.update { it.copy(tls13Ciphers = v) } }
            TextInputRow(stringResource(R.string.advanced_tls_groups), s.tlsGroups, placeholder = "P-256:X25519:P-384") { v -> vm.update { it.copy(tlsGroups = v) } }
            TextInputRow(stringResource(R.string.advanced_tls_key_shares), s.tlsKeyShares, placeholder = "X25519MLKEM768:X25519") { v -> vm.update { it.copy(tlsKeyShares = v) } }
            TextInputRow(stringResource(R.string.advanced_tls_sigalgs), s.tlsSigalgs, placeholder = "ecdsa_secp256r1_sha256:...") { v -> vm.update { it.copy(tlsSigalgs = v) } }
            TextInputRow(stringResource(R.string.advanced_tls_cert_compression), s.tlsCertCompression, placeholder = "zlib:brotli:zstd") { v -> vm.update { it.copy(tlsCertCompression = v) } }
            TextInputRow(stringResource(R.string.advanced_tls_masque_sni), s.masqueSni, placeholder = "consumer-masque.cloudflareclient.com") { v -> vm.update { it.copy(masqueSni = v) } }
            SwitchRow(stringResource(R.string.advanced_tls_grease), checked = s.tlsGrease) { v -> vm.update { it.copy(tlsGrease = v) } }
            SwitchRow(stringResource(R.string.advanced_tls_grease_sigalgs), checked = s.tlsGreaseSigalgs) { v -> vm.update { it.copy(tlsGreaseSigalgs = v) } }
            SwitchRow(stringResource(R.string.advanced_tls_ocsp), checked = s.tlsOcsp) { v -> vm.update { it.copy(tlsOcsp = v) } }
            SwitchRow(stringResource(R.string.advanced_tls_sct), checked = s.tlsSct) { v -> vm.update { it.copy(tlsSct = v) } }
            SwitchRow(stringResource(R.string.advanced_tls_session_ticket), checked = s.tlsSessionTicket) { v -> vm.update { it.copy(tlsSessionTicket = v) } }
            SwitchRow(stringResource(R.string.advanced_tls_permutation), checked = s.tlsPermutation) { v -> vm.update { it.copy(tlsPermutation = v) } }
            SwitchRow(stringResource(R.string.advanced_tls_verify), checked = s.tlsVerify) { v -> vm.update { it.copy(tlsVerify = v) } }
        }

        // ------------------------------------------------------ Zero Trust
        SettingsSection(stringResource(R.string.advanced_zerotrust_section)) {
            TextInputRow(stringResource(R.string.advanced_team), s.team, placeholder = "acme") { v -> vm.update { it.copy(team = v) } }
            TextInputRow(stringResource(R.string.advanced_access_email), s.accessEmail, placeholder = "me@example.com") { v -> vm.update { it.copy(accessEmail = v) } }
            TextInputRow(stringResource(R.string.advanced_access_id), s.accessId, placeholder = "client id") { v -> vm.update { it.copy(accessId = v) } }
            TextInputRow(stringResource(R.string.advanced_access_secret), s.accessSecret, placeholder = "secret") { v -> vm.update { it.copy(accessSecret = v) } }
            SwitchRow(stringResource(R.string.advanced_gateway), checked = s.gateway) { v -> vm.update { it.copy(gateway = v) } }
        }

        // ------------------------------------------------------ Tor / Psiphon
        SettingsSection(stringResource(R.string.advanced_tor_section)) {
            TextInputRow(stringResource(R.string.advanced_tor_bridges), s.torBridges, stringResource(R.string.advanced_tor_bridges_desc), "obfs4 …", multiline = true) { v -> vm.update { it.copy(torBridges = v) } }
            TextInputRow(stringResource(R.string.advanced_tor_country), s.torCountry, placeholder = "ir") { v -> vm.update { it.copy(torCountry = v) } }
        }
        SettingsSection(stringResource(R.string.advanced_psiphon_section)) {
            TextInputRow(stringResource(R.string.advanced_psiphon_region), s.psiphonRegion, placeholder = "DE") { v -> vm.update { it.copy(psiphonRegion = v) } }
        }

        // ------------------------------------------------------ local proxies
        SettingsSection(stringResource(R.string.advanced_proxy_section)) {
            NumberInputRow(stringResource(R.string.advanced_socks_port), s.socksPort) { v ->
                vm.update { it.copy(socksPort = v.coerceIn(1024, 65535)) }
            }
            SwitchRow(
                title = stringResource(R.string.advanced_http_proxy),
                desc = stringResource(R.string.advanced_http_proxy_desc),
                checked = s.httpProxy,
                onChange = { v -> vm.update { it.copy(httpProxy = v) } },
            )
            if (s.httpProxy) {
                NumberInputRow(stringResource(R.string.advanced_http_port), s.httpPort) { v ->
                    vm.update { it.copy(httpPort = v.coerceIn(1024, 65535)) }
                }
            }
        }

        // ------------------------------------------------------ core limits
        SettingsSection(stringResource(R.string.advanced_limits_section)) {
            SwitchRow(
                title = stringResource(R.string.advanced_route_sniff),
                desc = stringResource(R.string.advanced_route_sniff_desc),
                checked = s.routeSniff,
                onChange = { v -> vm.update { it.copy(routeSniff = v) } },
            )
            NumberInputRow(stringResource(R.string.advanced_sniff_ms), s.sniffMs) { v ->
                vm.update { it.copy(sniffMs = v.coerceIn(0, 5000)) }
            }
            NumberInputRow(stringResource(R.string.advanced_tcp_connect_timeout), s.tcpConnectTimeout) { v ->
                vm.update { it.copy(tcpConnectTimeout = v.coerceIn(5, 300)) }
            }
            SwitchRow(
                title = stringResource(R.string.advanced_reprovision),
                desc = stringResource(R.string.advanced_reprovision_desc),
                checked = s.reprovision,
                onChange = { v -> vm.update { it.copy(reprovision = v) } },
            )
        }
    }

    if (showCarrier) {
        ChoiceDialog(
            title = stringResource(R.string.advanced_carrier),
            options = listOf(
                ChoiceOption(stringResource(R.string.advanced_carrier_h3)),
                ChoiceOption(stringResource(R.string.advanced_carrier_h2)),
            ),
            selectedIndex = MasqueCarrier.entries.indexOf(s.carrier),
            onDismiss = { showCarrier = false },
            onPick = { i ->
                vm.update { it.copy(carrier = MasqueCarrier.entries[i]) }
                showCarrier = false
            },
        )
    }
    if (showStack) {
        ChoiceDialog(
            title = stringResource(R.string.advanced_stack),
            options = listOf(
                ChoiceOption(stringResource(R.string.advanced_stack_userspace)),
                ChoiceOption(stringResource(R.string.advanced_stack_hybrid)),
            ),
            selectedIndex = StackMode.entries.indexOf(s.stackMode),
            onDismiss = { showStack = false },
            onPick = { i ->
                vm.update { it.copy(stackMode = StackMode.entries[i]) }
                showStack = false
            },
        )
    }
}
