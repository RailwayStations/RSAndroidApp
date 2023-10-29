package de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.chip.Chip
import de.bahnhoefe.deutschlands.bahnhofsfotos.CountryActivity
import de.bahnhoefe.deutschlands.bahnhofsfotos.R
import de.bahnhoefe.deutschlands.bahnhofsfotos.db.DbAdapter
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.PreferencesService
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.StationFilter
import java.util.stream.IntStream

class StationFilterBar(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val toggleSort: Chip
    private val photoFilter: Chip
    private val activeFilter: Chip
    private val nicknameFilter: Chip
    private val countrySelection: Chip
    private var listener: OnChangeListener? = null
    private lateinit var context: Context
    private lateinit var preferencesService: PreferencesService
    private lateinit var dbAdapter: DbAdapter
    private lateinit var activity: Activity

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    init {
        LayoutInflater.from(context).inflate(R.layout.station_filter_bar, this)
        toggleSort = findViewById(R.id.toggleSort)
        toggleSort.setOnClickListener { v: View -> showSortMenu(v) }
        photoFilter = findViewById(R.id.photoFilter)
        photoFilter.setOnClickListener { v: View -> showPhotoFilter(v) }
        activeFilter = findViewById(R.id.activeFilter)
        activeFilter.setOnClickListener { v: View -> showActiveFilter(v) }
        nicknameFilter = findViewById(R.id.nicknameFilter)
        nicknameFilter.setOnClickListener { selectNicknameFilter() }
        countrySelection = findViewById(R.id.countrySelection)
        countrySelection.setOnClickListener { selectCountry() }
    }

    private fun setCloseIcon(chip: Chip, icon: Int) {
        chip.closeIcon = AppCompatResources.getDrawable(context, icon)
    }

    private fun setChipStatus(chip: Chip, iconRes: Int, active: Boolean, textRes: Int) {
        setChipStatus(chip, iconRes, active, context.getString(textRes))
    }

    private fun setChipStatus(chip: Chip, iconRes: Int, active: Boolean, text: String?) {
        if (iconRes != 0) {
            chip.chipIcon =
                getTintedDrawable(
                    context,
                    iconRes,
                    getChipForegroundColor(active)
                )
        } else {
            chip.chipIcon = null
        }
        chip.setChipBackgroundColorResource(if (active) R.color.colorPrimary else R.color.fullTransparent)
        chip.setTextColor(getChipForegroundColor(active))
        chip.setCloseIconTintResource(getChipForegroundColorRes(active))
        chip.setChipStrokeColorResource(if (active) R.color.colorPrimary else R.color.chipForeground)
        chip.text = text
        chip.textEndPadding = 0f
        if (TextUtils.isEmpty(text)) {
            chip.textStartPadding = 0f
        } else {
            chip.textStartPadding =
                activity.resources.getDimension(R.dimen.chip_textStartPadding)
        }
    }

    private fun getTintedDrawable(context: Context, imageId: Int, color: Int): Drawable? {
        if (imageId > 0) {
            val unwrappedDrawable = ContextCompat.getDrawable(context, imageId)
            return getTintedDrawable(unwrappedDrawable, color)
        }
        return null
    }

    private fun getChipForegroundColor(active: Boolean): Int {
        return context.getColor(getChipForegroundColorRes(active))
    }

    private fun getChipForegroundColorRes(active: Boolean): Int {
        return if (active) R.color.colorOnPrimary else R.color.chipForeground
    }

    fun init(
        preferencesService: PreferencesService,
        dbAdapter: DbAdapter,
        activity: Activity
    ) {
        this.preferencesService = preferencesService
        this.dbAdapter = dbAdapter
        this.context = activity
        this.activity = activity
        if (activity is OnChangeListener) {
            listener = activity
        }
        val stationFilter = preferencesService.stationFilter
        setChipStatus(
            photoFilter,
            stationFilter.photoIcon,
            stationFilter.isPhotoFilterActive,
            R.string.no_text
        )
        setChipStatus(
            nicknameFilter,
            stationFilter.nicknameIcon,
            stationFilter.isNicknameFilterActive,
            stationFilter.getNicknameText(activity)
        )
        setChipStatus(
            activeFilter,
            stationFilter.activeIcon,
            stationFilter.isActiveFilterActive,
            stationFilter.activeText
        )
        setChipStatus(
            countrySelection,
            R.drawable.ic_countries_active_24px,
            true,
            getCountryText(preferencesService)
        )
        setSortOrder(preferencesService.sortByDistance)
    }

    private fun showActiveFilter(v: View) {
        val popup = PopupMenu(activity, v)
        popup.menuInflater.inflate(R.menu.active_filter, popup.menu)
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            val stationFilter = preferencesService.stationFilter
            when (menuItem.itemId) {
                R.id.active_filter_active -> {
                    stationFilter.isActive = java.lang.Boolean.TRUE
                }

                R.id.active_filter_inactive -> {
                    stationFilter.isActive = java.lang.Boolean.FALSE
                }

                else -> {
                    stationFilter.isActive = null
                }
            }
            setChipStatus(
                activeFilter,
                stationFilter.activeIcon,
                stationFilter.isActiveFilterActive,
                R.string.no_text
            )
            updateStationFilter(stationFilter)
            false
        }
        setPopupMenuIcons(popup)
        popup.setOnDismissListener {
            setCloseIcon(
                activeFilter,
                R.drawable.ic_baseline_arrow_drop_up_24
            )
        }
        popup.show()
        setCloseIcon(activeFilter, R.drawable.ic_baseline_arrow_drop_down_24)
    }

    private fun showPhotoFilter(v: View) {
        val popup = PopupMenu(activity, v)
        popup.menuInflater.inflate(R.menu.photo_filter, popup.menu)
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            val stationFilter = preferencesService.stationFilter
            when (menuItem.itemId) {
                R.id.photo_filter_has_photo -> {
                    stationFilter.setPhoto(java.lang.Boolean.TRUE)
                }

                R.id.photo_filter_without_photo -> {
                    stationFilter.setPhoto(java.lang.Boolean.FALSE)
                }

                else -> {
                    stationFilter.setPhoto(null)
                }
            }
            setChipStatus(
                photoFilter,
                stationFilter.photoIcon,
                stationFilter.isPhotoFilterActive,
                R.string.no_text
            )
            updateStationFilter(stationFilter)
            false
        }
        setPopupMenuIcons(popup)
        popup.setOnDismissListener {
            setCloseIcon(
                photoFilter,
                R.drawable.ic_baseline_arrow_drop_up_24
            )
        }
        popup.show()
        setCloseIcon(photoFilter, R.drawable.ic_baseline_arrow_drop_down_24)
    }

    private fun selectCountry() {
        context.startActivity(Intent(context, CountryActivity::class.java))
    }

    private fun showSortMenu(v: View) {
        val popup = PopupMenu(activity, v)
        popup.menuInflater.inflate(R.menu.sort_order, popup.menu)
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            val sortByDistance = menuItem.itemId == R.id.sort_order_by_distance
            setSortOrder(sortByDistance)
            preferencesService.sortByDistance = sortByDistance
            if (listener != null) {
                listener!!.sortOrderChanged(sortByDistance)
            }
            false
        }
        setPopupMenuIcons(popup)
        popup.setOnDismissListener {
            setCloseIcon(
                toggleSort,
                R.drawable.ic_baseline_arrow_drop_up_24
            )
        }
        popup.show()
        setCloseIcon(toggleSort, R.drawable.ic_baseline_arrow_drop_down_24)
    }

    @SuppressLint("RestrictedApi")
    private fun setPopupMenuIcons(popup: PopupMenu) {
        try {
            val menuBuilder = popup.menu
            if (menuBuilder is MenuBuilder) {
                menuBuilder.setOptionalIconsVisible(true)
                for (item in menuBuilder.visibleItems) {
                    val iconMarginPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        0f,
                        resources.displayMetrics
                    )
                    val icon =
                        InsetDrawable(item.icon, iconMarginPx, 0f, iconMarginPx, 0f)
                    icon.setTint(resources.getColor(R.color.colorSurface, null))
                    item.icon = icon
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting popupMenuIcons: ", e)
        }
    }

    fun setSortOrder(sortByDistance: Boolean) {
        setChipStatus(
            toggleSort,
            if (sortByDistance) R.drawable.ic_sort_by_distance_active_24px else R.drawable.ic_sort_by_alpha_active_24px,
            true,
            R.string.no_text
        )
    }

    private fun selectNicknameFilter() {
        val nicknames = dbAdapter.photographerNicknames
        val stationFilter = preferencesService.stationFilter
        if (nicknames.isEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.no_nicknames_found),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val selectedNickname = IntStream.range(0, nicknames.size)
            .filter { i: Int -> nicknames[i] == stationFilter.nickname }
            .findFirst().orElse(-1)
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialogCustom))
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.select_nickname)
            .setSingleChoiceItems(nicknames, selectedNickname, null)
            .setPositiveButton(R.string.button_ok_text) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                val selectedPosition = (dialog as AlertDialog).listView.checkedItemPosition
                if (selectedPosition >= 0 && nicknames.size > selectedPosition) {
                    stationFilter.nickname = nicknames[selectedPosition]
                    setChipStatus(
                        nicknameFilter,
                        stationFilter.nicknameIcon,
                        stationFilter.isNicknameFilterActive,
                        stationFilter.getNicknameText(context)
                    )
                    updateStationFilter(stationFilter)
                }
            }
            .setNeutralButton(R.string.button_remove_text) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                stationFilter.nickname = null
                setChipStatus(
                    nicknameFilter,
                    stationFilter.nicknameIcon,
                    stationFilter.isNicknameFilterActive,
                    stationFilter.getNicknameText(context)
                )
                updateStationFilter(stationFilter)
            }
            .setNegativeButton(R.string.button_myself_text) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
                stationFilter.nickname = preferencesService.nickname
                setChipStatus(
                    nicknameFilter,
                    stationFilter.nicknameIcon,
                    stationFilter.isNicknameFilterActive,
                    stationFilter.getNicknameText(context)
                )
                updateStationFilter(stationFilter)
            }
            .create().show()
    }

    private fun updateStationFilter(stationFilter: StationFilter) {
        preferencesService.stationFilter = stationFilter
        if (listener != null) {
            listener!!.stationFilterChanged(stationFilter)
        }
    }

    fun setSortOrderEnabled(enabled: Boolean) {
        toggleSort.visibility = if (enabled) VISIBLE else GONE
    }

    interface OnChangeListener {
        fun stationFilterChanged(stationFilter: StationFilter)
        fun sortOrderChanged(sortByDistance: Boolean)
    }

    companion object {
        private val TAG = StationFilterBar::class.java.simpleName
        private fun getTintedDrawable(unwrappedDrawable: Drawable?, color: Int): Drawable? {
            if (unwrappedDrawable != null) {
                val wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable)
                DrawableCompat.setTint(wrappedDrawable, color)
                return wrappedDrawable
            }
            return null
        }

        private fun getCountryText(preferencesService: PreferencesService): String {
            return preferencesService.countryCodes.joinToString(",")
        }
    }
}