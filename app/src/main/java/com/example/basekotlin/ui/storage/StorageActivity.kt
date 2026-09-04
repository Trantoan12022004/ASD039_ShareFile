package com.example.basekotlin.ui.storage

import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basekotlin.R
import com.example.basekotlin.base.BaseActivity
import com.example.basekotlin.base.gone
import com.example.basekotlin.base.tap
import com.example.basekotlin.base.visible
import com.example.basekotlin.databinding.ActivityStorageBinding
import com.example.basekotlin.databinding.PopupMoreStorage1Binding
import com.example.basekotlin.databinding.PopupMoreStorage2Binding
import com.example.basekotlin.databinding.PopupMoreStorageBinding
import com.example.basekotlin.model.StorageItem
import com.example.basekotlin.model.StorageSortOption
import com.example.basekotlin.ui.storage.adapter.StorageAdapter
import com.example.basekotlin.ui.storage.dialog.CreateFolderDialog
import com.example.basekotlin.ui.storage.dialog.DeleteFileDialog
import com.example.basekotlin.ui.storage.dialog.DeleteFolderDialog
import com.example.basekotlin.ui.storage.dialog.InformationStorageDialog
import com.example.basekotlin.ui.storage.dialog.RenameDialog
import com.example.basekotlin.ui.storage.dialog.SortStorageDialog
import com.example.basekotlin.util.PopupMenuUtils
import com.example.basekotlin.util.Utils
import kotlinx.coroutines.launch
import java.io.File


class StorageActivity : BaseActivity<ActivityStorageBinding>(ActivityStorageBinding::inflate) {
    private val viewModel: StorageViewModel by viewModels()
    private var storageAdapter = StorageAdapter()
    private var currentOptionSort : StorageSortOption = StorageSortOption.NAME_A_Z
    private var isSearchMode = false

    override fun initView() {
        super.initView()
        // Gán layoutManager mặc định là Linear
        binding.rvStorage.layoutManager = LinearLayoutManager(this)
        binding.rvStorage.adapter = storageAdapter

        binding.tvEmptyTitle.text = getString(R.string.no_file)
        binding.tvEmptyMessage.text = getString(R.string.no_file_desc)
        binding.ivEmpty.setImageResource(R.drawable.ic_no_file)

        // Gán callback cho adapter
        storageAdapter.onItemClick = { item ->
            onItemClicked(item)
        }

        storageAdapter.onMoreClick = { item, anchor ->
            if (item.isDirectory){
                showMoreMenu1(item, anchor)
            }else{
                showMoreMenu2(item, anchor)
            }
        }
    }

    override fun bindView() {
        super.bindView()
        // Nút Back
        binding.layoutToolbar.btnBack.tap {
            onBack()
        }
        // Nút Grid/List toggle
        binding.layoutToolbar.btnGridlayout.tap {
            viewModel.toggleGridMode()
        }

        // Nút More → hiển thị popup sort và create folder
        binding.layoutToolbar.btnMore.tap {
            showMoreMenu(binding.layoutToolbar.btnMore)
        }

        // Nút Search
        binding.layoutToolbar.btnSearch.tap {
            openSearch()
        }

        // Lắng nghe thay đổi nội dung ô tìm kiếm
        binding.layoutToolbar.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                viewModel.setSearchQuery(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Bắt đầu observe ViewModel
        observeViewModel()
    }

    private fun showMoreMenu(anchor: View) {
        PopupMenuUtils.showAnchoredMenu(
            anchor,
            inflateBinding = {inflater -> PopupMoreStorageBinding.inflate(inflater)}
        ){  popupBinding, popupWindow ->
            popupBinding.tvSort.tap {
                popupWindow.dismiss()
                showDialogSort()
            }
            popupBinding.tvCreateFolder.tap {
                popupWindow.dismiss()
                showCreateFolderDialog()
            }

        }
    }

    private fun showCreateFolderDialog(){
        val dialog = CreateFolderDialog(
            context = this,
            validate = { enteredName ->
                // Kiểm tra trùng tên folder đã tồn tại trong thư mục hiện tại
                val currentFolder = viewModel.currentFolder.value
                if (currentFolder != null) {
                    val candidateFolder = File(currentFolder, enteredName)
                    if (candidateFolder.exists()) {
                        getString(R.string.text_input_failed1)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        ) { folderName ->
            viewModel.createFolder(folderName)
        }
        dialog.show()
    }

    private fun showDialogSort() {
        val currentOption = viewModel.sortOption.value
        val dialog = SortStorageDialog(this, currentOption) { selectedOption ->
            viewModel.setSortOption(selectedOption)
        }
        dialog.show()
    }

    private fun showMoreMenu1(item: StorageItem, anchor: View) {
        PopupMenuUtils.showAnchoredMenu(
            anchor,
            inflateBinding = { inflater -> PopupMoreStorage1Binding.inflate(inflater) }
        ){  popupBinding, popupWindow ->
            popupBinding.tvRename.tap {
                popupWindow.dismiss()
                showRenameDialog(item)
            }
            popupBinding.tvDelete.tap{
                popupWindow.dismiss()
                showDeleteFolderDialog(item)
            }
            popupBinding.tvInfo.tap {
                popupWindow.dismiss()
                showInfoDialog(item)
            }
        }
    }
    private fun showInfoDialog(item: StorageItem) {
        InformationStorageDialog(this, item).show()

    }
    private fun showDeleteFolderDialog(item: StorageItem){
        val targetFile = listOf(File(item.path))
        DeleteFolderDialog(this, item){
            viewModel.deleteItems(targetFile)
        }.show()
    }

    private fun showDeleteFileDialog(item: StorageItem){
        val targetFile = listOf(File(item.path))
        DeleteFileDialog(this, item){
            viewModel.deleteItems(targetFile)
        }.show()
    }
    private fun showRenameDialog(item: StorageItem){
        val targetFile = File(item.path)
        val initName: String
        if(item.isDirectory){
            initName = targetFile.name
        } else {
            initName = targetFile.nameWithoutExtension
        }// Khởi tạo dialog đổi tên
        val dialog = RenameDialog(
            context = this,
            initText = initName,
            validate = { enteredName ->
                // Xác định tên file/folder đích sau khi đổi
                val candidateName: String
                if (item.isDirectory) {
                    candidateName = enteredName
                } else {
                    val fileExtension: String = targetFile.extension
                    if (fileExtension.isNotEmpty()) {
                        candidateName = "$enteredName.$fileExtension"
                    } else {
                        candidateName = enteredName
                    }
                }
                // Kiểm tra xem tên mới đã tồn tại trong thư mục cha hay chưa
                val parentDir: File? = targetFile.parentFile
                if (parentDir != null) {
                    val destinationFile = File(parentDir, candidateName)
                    val isSameFile: Boolean =
                        destinationFile.absolutePath == targetFile.absolutePath
                    if (destinationFile.exists() && !isSameFile) {
                        // Trả về thông báo tên đã tồn tại
                        getString(R.string.text_input_failed1)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        ) { enteredNewName ->
            // Ghép lại phần mở rộng nếu là file
            val finalNewName: String
            if (item.isDirectory) {
                finalNewName = enteredNewName
            } else {
                val fileExtension: String = targetFile.extension
                if (fileExtension.isNotEmpty()) {
                    finalNewName = "$enteredNewName.$fileExtension"
                } else {
                    finalNewName = enteredNewName
                }
            }
            // Gọi ViewModel với đối tượng targetFile kiểu File và tên mới
            viewModel.renameItem(targetFile, finalNewName)
        }
        // Hiển thị dialog
        dialog.show()
    }
    private fun showMoreMenu2(item: StorageItem, anchor: View) {
        PopupMenuUtils.showAnchoredMenu(
            anchor,
            inflateBinding = { inflater -> PopupMoreStorage2Binding.inflate(inflater) }
        ){  popupBinding, popupWindow ->
            popupBinding.tvShare.tap {
                popupWindow.dismiss()
                shareItem(item)
            }
            popupBinding.tvInfo.tap {
                popupWindow.dismiss()
                showInfoDialog(item)
            }
            popupBinding.tvRename.tap { 
                popupWindow.dismiss()
                showRenameDialog(item)
            }
            popupBinding.tvDelete.tap {
                popupWindow.dismiss()
                showDeleteFileDialog(item)
            }
        }
    }

    private fun shareItem(item: StorageItem) {
        val file = File(item.path)
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)

        val intent: Intent = Intent(Intent.ACTION_SEND)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.send)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.send), Toast.LENGTH_SHORT).show()
        }
    }



    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Lắng nghe danh sách item hiển thị (đã filter theo search)
                launch {
                    viewModel.filteredItems.collect { items ->
                        updateList(items)
                    }
                }
                // Lắng nghe thông tin dung lượng bộ nhớ
                launch {
                    viewModel.storageModel.collect { model ->
                        binding.layoutStorage.tvStorageTitle.text = getString(R.string.used_storage)
                        binding.layoutStorage.tvStorageInfo.text = model.formattedDisplay
                        binding.layoutStorage.progressStorage.progress = model.usedPercentage
                    }
                }
                // Lắng nghe thư mục hiện tại → cập nhật breadcrumb
                launch {
                    viewModel.currentFolder.collect { folder ->
                        if (folder != null) {
                            renderBreadcrumb()
                            updateStorageVisibility(folder)
                        }
                    }
                }
                // Lắng nghe trạng thái loading
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        // TODO: Hiển thị/ẩn ProgressBar nếu có
                    }
                }
                // Lắng nghe Grid/List mode
                launch {
                    viewModel.isGridMode.collect { isGrid ->
                        updateLayoutMode(isGrid)
                    }
                }
                launch {
                    viewModel.sortOption.collect { sortOption ->
                        currentOptionSort = sortOption
                    }
                }
                // Lắng nghe kết quả thao tác (rename/create/delete) → Toast
                launch {
                    viewModel.operationResult.collect { result ->
                        when (result) {
                            true -> {
                                Toast.makeText(this@StorageActivity, getString(R.string.action_success), Toast.LENGTH_SHORT).show()
                                viewModel.clearOperationResult()
                            }
                            false -> {
                                Toast.makeText(this@StorageActivity, getString(R.string.action_failed), Toast.LENGTH_SHORT).show()
                                viewModel.clearOperationResult()
                            }
                            null -> { /* Chưa có thao tác */ }
                        }
                    }
                }
            }
        }
    }

    private fun openFileWithExternalApp(item: StorageItem) {
        val file = File(item.path)
        if (!file.exists()) {
            return
        }
        val uri = FileProvider.getUriForFile(this, packageName + ".provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, contentResolver.getType(uri) ?: "*/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.no_app_to_open_file), Toast.LENGTH_SHORT).show()
        }
    }

    private fun onItemClicked(item: StorageItem) {
        if (item.isDirectory) {
            // Mở thư mục con → push vào back stack
            val folder = File(item.path)
            viewModel.loadFolder(folder, pushToStack = true)
        } else {
            // Mở file bằng app mặc định của hệ thống
            openFileWithExternalApp(item)
        }
    }
    private fun updateLayoutMode(isGrid: Boolean) {
        // Cập nhật chế độ hiển thị trong Adapter để chọn layout item_doc_card_expand hoặc item_doc_card
        storageAdapter.isGridMode = isGrid

        if (isGrid) {
            // Hiển thị dạng lưới 2 cột
            binding.rvStorage.layoutManager = GridLayoutManager(this, 2)
            binding.layoutToolbar.btnGridlayout.setImageResource(R.drawable.ic_list)
        } else {
            // Hiển thị dạng danh sách Linear
            binding.rvStorage.layoutManager = LinearLayoutManager(this)
            binding.layoutToolbar.btnGridlayout.setImageResource(R.drawable.ic_gird)
        }
        // Gán lại adapter sau khi đổi LayoutManager
        binding.rvStorage.adapter = storageAdapter
    }

    private fun updateList(items: List<StorageItem>) {
        if (items.isEmpty()) {
            binding.rvStorage.gone()
            binding.layoutEmpty.visible()
        } else {
            binding.rvStorage.visible()
            binding.layoutEmpty.gone()
            storageAdapter.addListData(items.toMutableList())
        }
    }

    private fun renderBreadcrumb() {
        val breadcrumbContainer = binding.layoutToolbar.layoutBreadcrumb
        // Xóa toàn bộ breadcrumb cũ (giữ lại item "Files >" cố định ở index 0)
        val fixedChildCount = 1
        if (breadcrumbContainer.childCount > fixedChildCount) {
            breadcrumbContainer.removeViews(fixedChildCount, breadcrumbContainer.childCount - fixedChildCount)
        }

        val breadcrumbList = viewModel.getBreadcrumbList()

        // Tính chiều cao 1 dòng breadcrumb để xác định max height cho 3 dòng
        val singleLineHeight = resources.getDimensionPixelSize(R.dimen.breadcrumb_line_height) // khoảng 28dp
        val maxLines = 3

        // Thêm toàn bộ breadcrumb items (bỏ qua item đầu vì đã có "Files >" cố định)
        for (index in breadcrumbList.indices) {
            val folder = breadcrumbList[index]
            val isLast = index == breadcrumbList.lastIndex
            // Bỏ qua root vì đã có item cố định trong XML


            val itemBinding = com.example.basekotlin.databinding.ItemBreadcrumbsBinding
                .inflate(LayoutInflater.from(this), breadcrumbContainer, false)
            if (folder.absolutePath == viewModel.rootDirectory.absolutePath) {
                itemBinding.tvBreadcrumbName.text = getString(R.string.internal_shared_storage)
            } else {
                itemBinding.tvBreadcrumbName.text = folder.name
            }


            if (isLast) {
                itemBinding.tvBreadcrumbName.setTextColor(getColor(R.color.primary_35))
                itemBinding.ivArrow.gone()
            } else {
                itemBinding.tvBreadcrumbName.setTextColor(getColor(R.color.black))
                itemBinding.ivArrow.visible()
                itemBinding.root.tap {
                    viewModel.navigateTo(folder)
                }
            }
            breadcrumbContainer.addView(itemBinding.root)
        }

        // Sau khi render, đợi layout xong rồi kiểm tra xem có vượt 3 dòng không
        breadcrumbContainer.post {
            val measuredHeight = breadcrumbContainer.height
            val maxHeight = singleLineHeight * maxLines

            if (measuredHeight > maxHeight) {
                // Vượt 3 dòng → truncate: giữ item "Files >" (index 0) + "Internal shared storage" (index 1) + thêm "..." + giữ các item cuối
                truncateBreadcrumb(breadcrumbContainer, maxHeight, singleLineHeight)
            }
        }
    }

    private fun truncateBreadcrumb(container: com.google.android.flexbox.FlexboxLayout, maxHeight: Int, singleLineHeight: Int) {
        // Giữ item cố định "Files >" (index 0) và item "Internal shared storage" (index 1)
        // Thêm "..." ở index 2
        // Xóa các item ở giữa cho đến khi fit <= 3 dòng

        // Tạo item "..."
        val ellipsisBinding = com.example.basekotlin.databinding.ItemBreadcrumbsBinding
            .inflate(LayoutInflater.from(this), container, false)
        ellipsisBinding.tvBreadcrumbName.text = getString(R.string.ellipsis) // "..."
        ellipsisBinding.tvBreadcrumbName.setTextColor(getColor(R.color.black))
        ellipsisBinding.ivArrow.visible()

        // Lưu lại 2 item đầu và các item cuối
        val keepStart = 2 // "Files >" và "Internal shared storage >"
        val totalChildren = container.childCount

        // Xóa dần item ở giữa (sau vị trí keepStart) và thêm "..." cho đến khi fit
        while (container.childCount > keepStart + 1) { // +1 vì giữ ít nhất item cuối
            // Xóa item ở vị trí keepStart (item ngay sau "Internal shared storage")
            container.removeViewAt(keepStart)
            container.addView(ellipsisBinding.root, keepStart)

            // Đo lại
            container.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(container.width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            )

            if (container.measuredHeight <= maxHeight) {
                break
            }

            // Nếu vẫn vượt, xóa "..." và tiếp tục xóa item tiếp theo
            container.removeViewAt(keepStart)
        }

        // Nếu chưa có "..." thì thêm vào
        if (container.getChildAt(keepStart) !== ellipsisBinding.root) {
            container.addView(ellipsisBinding.root, keepStart)
        }
    }

    private fun updateStorageVisibility(currentFolder: File) {
        // Kiểm tra xem thư mục hiện tại có trùng với thư mục gốc (root) hay không
        val isAtRoot = currentFolder.absolutePath == viewModel.rootDirectory.absolutePath
        if (isAtRoot) {
            // Đang ở thư mục root → hiển thị card dung lượng bộ nhớ
            binding.layoutStorage.root.visible()
        } else {
            // Đang ở thư mục con → ẩn card dung lượng bộ nhớ đi
            binding.layoutStorage.root.gone()
        }
    }


    // Mở chế độ tìm kiếm
    private fun openSearch() {
        if (isSearchMode) return
        isSearchMode = true



        // Hiển thị ô search và focus bàn phím
        binding.layoutToolbar.layoutSearch.visible()
        binding.layoutToolbar.edtSearch.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.layoutToolbar.edtSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    // Đóng chế độ tìm kiếm
    private fun closeSearch() {
        if (!isSearchMode) return
        isSearchMode = false

        // Xóa query tìm kiếm
        viewModel.setSearchQuery("")
        binding.layoutToolbar.edtSearch.setText("")
        Utils.hideKeyboard(this)

        // Ẩn ô search, hiển thị lại breadcrumb và các nút action
        binding.layoutToolbar.layoutSearch.gone()
    }

    override fun onBack() {
        if (isSearchMode) {
            // Đang tìm kiếm → đóng tìm kiếm
            closeSearch()
        } else {
            val navigatedUp = viewModel.navigateUp()
            if (!navigatedUp) {
                // Đã ở root → thoát Activity
                finishThisActivity()
            }
        }
    }


}