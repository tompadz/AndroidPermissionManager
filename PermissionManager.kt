import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/**
 * A manager that simplifies working with requests [Manifest.permission].
 *
 * Works by calling [PermissionManager.actions], which
 * will return the result in [Boolean] format.
 *
 * Can only be created in [Fragment] and must be
 * initialized before calling [Fragment.onCreate].
 *
 * @property action the action that will be performed when requesting permission
 * @property cameraPermissions is a set of secure permissions,
 * security consists in a set of permissions for different SDK versions,
 * which guarantees the correct result on different devices.
 *
 * @param fragment the fragment in which you need to request permissions
 *
 * @see PermissionAction
 * @see PermissionManager.action
 */
class PermissionManager(fragment: Fragment) {

    private val actions: MutableMap<Array<String>, ((result: Boolean) -> Unit)> = mutableMapOf()

    /**
     * The action performed when receiving a response from the permission.
     *
     * ```
     * permissionManager.action(
     *    Manifest.permission.CAMERA
     *  ) { isSuccesses ->
     *      println(isSuccesses.toString())
     *  }.start()
     * ```
     *
     * @param permissions a list of permissions to be answered.
     * @param action an action with the result of a permission to be performed
     *
     * @return [PermissionAction] which can trigger a request for permissions
     *
     * @see PermissionAction
     */
    fun action(vararg permissions: String, action: (result: Boolean) -> Unit): PermissionAction {
        val permission = permissions.toSet().toTypedArray()
        actions[permission] = action
        return PermissionAction(permission)
    }

    /**
     * The action performed when receiving a response from the permission.
     * ```
     * permissionManager.action(
     *      permissions = { it.cameraPermissions },
     *      action = { isSuccesses -> println(isSuccesses.toString()) }
     *  ).start()
     * ```
     * @param permissions a list of permissions to be answered.
     * @param action an action with the result of a permission to be performed
     *
     * @return [PermissionAction] which can trigger a request for permissions
     *
     * @see PermissionAction
     */
    fun action(permissions: (pm: PermissionManager) -> Array<String>, action: (result: Boolean) -> Unit): PermissionAction {
        val permission = permissions.invoke(this)
        actions[permission] = action
        return PermissionAction(permission)
    }

    /**
     * Set of permissions to access the camera and
     * additional permissions for her work. The set of permissions may vary
     * depending on the SDK version.
     *
     * For SDK 29 and below, permissions will be called:
     * ```
     * Manifest.permission.CAMERA,
     * Manifest.permission.READ_EXTERNAL_STORAGE,
     * Manifest.permission.WRITE_EXTERNAL_STORAGE
     * ```
     * For SDK above 29, a call is sufficient:
     * ```
     * Manifest.permission.CAMERA
     * ```
     */
    val cameraPermissions get() = getSafeCameraPermissions()

    private val permissionResult = fragment.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val isSuccesses = result.all { it.value }
        val permissions = result.keys.toList()
        actions.mapKeys { it.key.toList() }[permissions]?.invoke(isSuccesses)
    }

    private fun getSafeCameraPermissions() : Array<String> {
        val isNeedCheckStoragePermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
        return if (isNeedCheckStoragePermission) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            arrayOf(Manifest.permission.CAMERA)
        }
    }

    /**
     * A class for calling a permission request
     * @property start Start permission request
     */
    inner class PermissionAction (private val permission: Array<String>) {
        /**
         * Start permission request
         */
        fun start() { permissionResult.launch(permission) }
    }
}

